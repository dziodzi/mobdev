import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../data/chat_api.dart';
import '../data/local_store.dart';
import '../data/models.dart';

class ChannelState {
  ChannelState(this.name);

  final String name;
  final messages = <int, Message>{};
  final outgoing = <Outgoing>[];
  final sentIds = <int>{};
  String draft = '';
  int draftRevision = 0;
  int? anchorId;
  int? olderCursor;
  int? newerCursor;
  bool loaded = false;
  bool loading = false;
  bool hasOlder = false;
  Problem? problem;
  bool olderFailed = false;
  double scrollOffset = 0;
  bool get sending => outgoing.any((item) => item.delivery == Delivery.sending);

  Map<String, dynamic> toJson() => {
    'draft': draft,
    'outgoing': outgoing.map((item) => item.toJson()).toList(),
  };
}

class ChatState extends ChangeNotifier {
  ChatState(this.api, this._store);

  final ChatApi api;
  final LocalStore _store;
  final _chats = <String, ChannelState>{};
  Future<void> _writes = Future.value();
  bool _disposed = false;

  Session? session;
  bool starting = true;
  bool authenticating = false;
  Problem? loginProblem;
  Problem? storageProblem;
  List<String> channels = const [];
  bool channelsLoaded = false;
  bool loadingChannels = false;
  Problem? channelsProblem;
  ChannelState? selected;

  @override
  void notifyListeners() {
    if (!_disposed) super.notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    session = null;
    api.close();
    super.dispose();
  }

  Future<void> restore() async {
    try {
      final saved = await _store.readSession();
      if (saved != null) await _open(saved, remember: false);
    } on Exception {
      loginProblem = Problem.storage;
    } finally {
      starting = false;
      notifyListeners();
    }
  }

  Future<void> signIn(String user, String password) async {
    if (starting || authenticating || session != null) return;
    authenticating = true;
    loginProblem = null;
    notifyListeners();
    try {
      final signedIn = await api.login(user, password);
      if (!_disposed) await _open(signedIn, remember: true);
    } on ApiException catch (error) {
      loginProblem = error.problem;
    } on Exception {
      loginProblem = Problem.storage;
    } finally {
      authenticating = false;
      notifyListeners();
    }
  }

  Future<void> _open(Session next, {required bool remember}) async {
    final stored = await _store.readUser(next.user);
    final restored = <String, ChannelState>{};
    for (final MapEntry(:key, :value) in stored.entries) {
      if (value is! Map<String, dynamic>) continue;
      final chat = ChannelState(key)..draft = value['draft'] as String? ?? '';
      final outbox = value['outgoing'] as List? ?? const [];
      chat.outgoing.addAll(
        outbox.map((item) => Outgoing.fromJson(item as Map<String, dynamic>)),
      );
      restored[key] = chat;
    }
    if (_disposed) return;
    if (remember) await _store.saveSession(next);
    if (_disposed) return;
    _chats.addAll(restored);
    session = next;
    storageProblem = null;
    unawaited(loadChannels());
  }

  Future<void> signOut() async {
    final current = session;
    if (current == null) return;
    await _endSession(current, clearDrafts: true);
    unawaited(api.logout(current).catchError((Object _) {}));
  }

  Future<void> _endSession(Session current, {required bool clearDrafts}) async {
    if (!identical(session, current)) return;
    session = null;
    selected = null;
    channels = const [];
    channelsLoaded = false;
    loadingChannels = false;
    channelsProblem = null;
    _chats.clear();
    authenticating = true;
    loginProblem = clearDrafts ? null : Problem.unauthorized;
    notifyListeners();
    try {
      await _writes;
      await Future.wait([
        _store.deleteSession(),
        if (clearDrafts) _store.deleteUser(current.user),
      ]);
    } on Exception {
      loginProblem = Problem.storage;
    } finally {
      authenticating = false;
      notifyListeners();
    }
  }

  Future<void> loadChannels() async {
    final current = session;
    if (current == null || loadingChannels) return;
    loadingChannels = true;
    channelsProblem = null;
    notifyListeners();
    try {
      final result = await api.channels();
      if (!identical(session, current)) return;
      channels = List.unmodifiable(
        result.toSet().toList()
          ..sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase())),
      );
      channelsLoaded = true;
    } on ApiException catch (error) {
      if (identical(session, current)) channelsProblem = error.problem;
    } finally {
      if (identical(session, current)) {
        loadingChannels = false;
        notifyListeners();
      }
    }
  }

  void select(String channel) {
    if (session == null) return;
    final chat = _chats.putIfAbsent(channel, () => ChannelState(channel));
    selected = chat;
    notifyListeners();
    if (!chat.loaded && !chat.loading && chat.problem == null) {
      unawaited(loadHistory(chat));
    }
  }

  void closeChat() {
    selected = null;
    notifyListeners();
  }

  Future<void> loadHistory(ChannelState chat, {bool older = false}) async {
    final current = session;
    if (current == null || chat.loading || (older && !chat.hasOlder)) return;
    chat.loading = true;
    chat.problem = null;
    chat.olderFailed = false;
    notifyListeners();
    try {
      if (!chat.loaded || older) {
        final cursor = older ? chat.olderCursor : null;
        final page = await api.history(chat.name, cursor: cursor);
        if (!identical(session, current)) return;
        final olderItems = page
            .where((message) => cursor == null || message.id < cursor)
            .toList();
        _merge(chat, olderItems);
        if (olderItems.isNotEmpty) {
          final ids = olderItems.map((message) => message.id).toList()..sort();
          chat.olderCursor = ids.first;
          chat.newerCursor ??= ids.last;
        }
        chat.loaded = true;
        chat.hasOlder =
            page.length == ChatApi.pageSize && olderItems.isNotEmpty;
      } else {
        var cursor = chat.newerCursor ?? 0;
        while (true) {
          final page = await api.history(
            chat.name,
            cursor: cursor,
            reverse: false,
          );
          if (!identical(session, current)) return;
          final added = page.where((message) => message.id > cursor).toList();
          if (added.isEmpty) break;
          _merge(chat, added);
          cursor = added
              .map((message) => message.id)
              .reduce((a, b) => a > b ? a : b);
          chat.newerCursor = cursor;
          if (page.length < ChatApi.pageSize) break;
        }
      }
    } on ApiException catch (error) {
      if (identical(session, current)) {
        chat.problem = error.problem;
        chat.olderFailed = older;
      }
    } finally {
      if (identical(session, current)) {
        chat.loading = false;
        notifyListeners();
      }
    }
  }

  void _merge(ChannelState chat, List<Message> messages) {
    if (messages.isEmpty) return;
    chat.anchorId ??= messages
        .map((message) => message.id)
        .reduce((a, b) => a > b ? a : b);
    for (final message in messages) {
      chat.messages[message.id] = message;
    }
  }

  void editDraft(ChannelState chat, String text) {
    final current = session;
    if (current == null || chat.draft == text) return;
    chat.draft = text;
    chat.draftRevision++;
    unawaited(_persist(current));
    notifyListeners();
  }

  void returnToEditor(ChannelState chat, Outgoing item) {
    if (chat.draft == item.text) return;
    editDraft(
      chat,
      chat.draft.trim().isEmpty ? item.text : '${chat.draft}\n${item.text}',
    );
  }

  Outgoing? failedDraft(ChannelState chat) {
    for (final item in chat.outgoing.reversed) {
      if (item.delivery != Delivery.sending && item.text == chat.draft) {
        return item;
      }
    }
    return null;
  }

  Future<void> send(ChannelState chat, {Outgoing? retry}) async {
    final current = session;
    final text = retry?.text ?? chat.draft;
    if (current == null || chat.sending || text.trim().isEmpty) return;
    final revision = chat.draftRevision;
    final now = DateTime.now();
    final item =
        retry?.withDelivery(Delivery.sending) ??
        Outgoing(
          now.microsecondsSinceEpoch.toString(),
          text,
          now,
          Delivery.sending,
        );
    final index = retry == null
        ? chat.outgoing.length
        : chat.outgoing.indexOf(retry);
    if (index < 0) return;
    if (retry == null) {
      chat.outgoing.add(item);
    } else {
      chat.outgoing[index] = item;
    }
    notifyListeners();
    try {
      final saved = await _persist(current);
      if (!identical(session, current)) return;
      if (!saved) throw const ApiException(Problem.storage);
      final id = await api.send(current, chat.name, text);
      if (!identical(session, current)) return;
      chat.outgoing.removeAt(index);
      chat.sentIds.add(id);
      chat.messages.putIfAbsent(
        id,
        () =>
            Message(id, current.user, chat.name, item.time, TextContent(text)),
      );
      chat.anchorId ??= id;
      if (chat.draftRevision == revision && chat.draft == text) {
        chat.draft = '';
        chat.draftRevision++;
      }
    } on ApiException catch (error) {
      if (!identical(session, current)) return;
      chat.outgoing[index] = item.withDelivery(
        error.problem == Problem.rejected || error.problem == Problem.storage
            ? Delivery.rejected
            : Delivery.uncertain,
        error.problem,
      );
      if (error.problem == Problem.unauthorized) {
        unawaited(_persist(current));
        await _endSession(current, clearDrafts: false);
        return;
      }
    }
    unawaited(_persist(current));
    notifyListeners();
  }

  Future<bool> _persist(Session current) {
    final snapshot = jsonEncode({
      for (final chat in _chats.values)
        if (chat.draft.isNotEmpty || chat.outgoing.isNotEmpty)
          chat.name: chat.toJson(),
    });
    final result = _writes.then((_) async {
      try {
        await _store.saveUser(current.user, snapshot);
        if (identical(session, current) && storageProblem != null) {
          storageProblem = null;
          notifyListeners();
        }
        return true;
      } on Exception {
        if (identical(session, current)) {
          storageProblem = Problem.storage;
          notifyListeners();
        }
        return false;
      }
    });
    _writes = result.then((_) {});
    return result;
  }
}
