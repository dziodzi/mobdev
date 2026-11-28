import 'package:flutter/foundation.dart';

import '../data/api_client.dart';
import '../data/local_store.dart';
import '../models/models.dart';

class Conversation {
  List<Message> messages = [];
  final List<Outgoing> outgoing = [];
  String? next;
  bool initialized = false, busy = false, sending = false;
  Failure? error;
  double offset = 0;
}

class ChatState extends ChangeNotifier {
  final ApiClient api;
  final LocalStore store;
  ChatState(this.api, this.store);
  Session? session;
  bool booting = true, signingIn = false, loadingChannels = false;
  Failure? error, channelsError, storageError;
  List<Channel> channels = [];
  Channel? selected;
  String? openedImage;
  Map<String, String> drafts = {};
  final Map<String, int> _revisions = {};
  final Map<String, Conversation> chats = {};
  final Map<String, Future<Uint8List>> _images = {};
  int _epoch = 0, _local = 0;
  Conversation conversation(String id) =>
      chats.putIfAbsent(id, Conversation.new);
  Future<void> start() async {
    try {
      session = await store.session();
      if (session != null) drafts = await store.drafts(session!.userId);
    } catch (_) {
      error = Failure.storage;
      session = null;
    }
    booting = false;
    notifyListeners();
    if (session != null) await loadChannels();
  }

  Future<void> login(String login, String password) async {
    if (signingIn) return;
    signingIn = true;
    error = null;
    notifyListeners();
    final epoch = _epoch;
    try {
      final s = await api.login(login, password);
      if (epoch != _epoch) return;
      await store.saveSession(s);
      final saved = await store.drafts(s.userId);
      if (epoch != _epoch) return;
      session = s;
      drafts = saved;
    } on ApiException catch (e) {
      error = e.kind;
    } catch (_) {
      error = Failure.storage;
    } finally {
      signingIn = false;
      notifyListeners();
    }
    if (session != null) await loadChannels();
  }

  Future<void> logout({bool expired = false}) async {
    final user = session?.userId;
    final oldToken = session?.token;
    _epoch++;
    session = null;
    selected = null;
    openedImage = null;
    drafts = {};
    chats.clear();
    _images.clear();
    channels = [];
    loadingChannels = false;
    channelsError = null;
    storageError = null;
    _revisions.clear();
    error = expired ? Failure.expired : null;
    notifyListeners();
    try {
      await store.clear(user);
      if (!expired && oldToken != null) {
        try {
          await api.logout(oldToken);
        } on ApiException {
          /* Local logout remains effective offline. */
        }
      }
    } catch (_) {
      error = Failure.storage;
      notifyListeners();
    }
  }

  void _failure(ApiException e, int epoch, void Function(Failure) set) {
    if (epoch != _epoch) return;
    if (e.kind == Failure.expired) {
      logout(expired: true);
    } else {
      set(e.kind);
    }
  }

  Future<void> loadChannels() async {
    if (session == null || loadingChannels) return;
    final epoch = _epoch, token = session!.token;
    loadingChannels = true;
    channelsError = null;
    notifyListeners();
    try {
      final result = await api.channels(token);
      if (epoch == _epoch) channels = result;
    } on ApiException catch (e) {
      _failure(e, epoch, (f) => channelsError = f);
    } finally {
      if (epoch == _epoch) {
        loadingChannels = false;
        notifyListeners();
      }
    }
  }

  void select(Channel c) {
    selected = c;
    notifyListeners();
    if (!conversation(c.id).initialized) loadMessages(c.id);
  }

  void back() {
    if (openedImage != null) {
      openedImage = null;
    } else {
      selected = null;
    }
    notifyListeners();
  }

  void openImage(String id) {
    openedImage = id;
    notifyListeners();
  }

  void draft(String id, String text) {
    drafts[id] = text;
    _revisions[id] = (_revisions[id] ?? 0) + 1;
    final s = session;
    if (s == null) return;
    store.saveDrafts(s.userId, drafts).catchError((Object _) {
      if (session?.userId == s.userId) {
        storageError = Failure.storage;
        notifyListeners();
      }
    });
  }

  Future<void> loadMessages(String id, {bool older = false}) async {
    if (session == null) return;
    final c = conversation(id);
    if (c.busy || (older && c.next == null)) return;
    final epoch = _epoch, token = session!.token;
    c.busy = true;
    c.error = null;
    notifyListeners();
    try {
      final page = await api.messages(token, id, older ? c.next : null);
      if (epoch != _epoch) return;
      final byId = {for (final m in c.messages) m.id: m};
      for (final m in page.messages) {
        byId[m.id] = m;
      }
      c.messages = byId.values.toList()
        ..sort((a, b) => int.parse(b.id).compareTo(int.parse(a.id)));
      if (older || !c.initialized) c.next = page.next;
      c.initialized = true;
    } on ApiException catch (e) {
      _failure(e, epoch, (f) => c.error = f);
    } finally {
      if (epoch == _epoch) {
        c.busy = false;
        notifyListeners();
      }
    }
  }

  Future<void> send(String id, {String? retryId}) async {
    if (session == null) return;
    final c = conversation(id);
    if (c.sending) return;
    final index = retryId == null
        ? -1
        : c.outgoing.indexWhere((m) => m.localId == retryId);
    final text = index < 0 ? (drafts[id] ?? '') : c.outgoing[index].text;
    if (text.trim().isEmpty) return;
    final revision = _revisions[id] ?? 0;
    final localId = index < 0 ? 'local-${++_local}' : c.outgoing[index].localId;
    final outgoing = Outgoing(localId, text, Delivery.sending);
    if (index < 0) {
      c.outgoing.add(outgoing);
    } else {
      c.outgoing[index] = outgoing;
    }
    c.sending = true;
    final epoch = _epoch, token = session!.token;
    notifyListeners();
    try {
      final result = await api.send(token, id, text, session!.userId);
      if (epoch != _epoch) return;
      c.outgoing[c.outgoing.indexWhere((m) => m.localId == localId)] = Outgoing(
        localId,
        text,
        Delivery.sent,
        result.id,
      );
      c.messages.removeWhere((m) => m.id == result.id);
      c.messages.insert(0, result);
      if ((_revisions[id] ?? 0) == revision && drafts[id] == text) {
        draft(id, '');
      }
    } on ApiException catch (e) {
      if (epoch != _epoch) return;
      c.outgoing[c.outgoing.indexWhere((m) => m.localId == localId)] = Outgoing(
        localId,
        text,
        Delivery.uncertain,
      );
      _failure(e, epoch, (f) => c.error = f);
    } finally {
      if (epoch == _epoch) {
        c.sending = false;
        notifyListeners();
      }
    }
  }

  void restore(String id, Outgoing message) {
    final current = drafts[id] ?? '';
    draft(
      id,
      current == message.text
          ? current
          : current.isEmpty
          ? message.text
          : '$current\n${message.text}',
    );
    notifyListeners();
  }

  Future<Uint8List> image(String id, {bool thumb = false}) {
    final key = "${thumb ? 'thumb' : 'img'}:$id";
    return _images.putIfAbsent(
      key,
      () => _fetchImage(id, thumb: thumb).onError((error, stack) {
        _images.remove(key);
        Error.throwWithStackTrace(error!, stack);
      }),
    );
  }

  Future<Uint8List> _fetchImage(String id, {bool thumb = false}) async {
    final epoch = _epoch;
    final s = session;
    if (s == null) throw const ApiException(Failure.expired);
    try {
      return await api.image(s.token, id, thumb: thumb);
    } on ApiException catch (e) {
      _failure(e, epoch, (_) {});
      rethrow;
    }
  }

  @override
  void dispose() {
    api.close();
    super.dispose();
  }
}
