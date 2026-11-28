class Session {
  final String userId;
  final String token;
  const Session(this.userId, this.token);
  factory Session.fromJson(Map<String, dynamic> j) =>
      Session(j['userId'] as String, j['token'] as String);
  Map<String, dynamic> toJson() => {'userId': userId, 'token': token};
}

class Channel {
  final String id;
  final String name;
  const Channel(this.id, this.name);
  factory Channel.fromJson(Map<String, dynamic> j) =>
      Channel(j['id'] as String, j['name'] as String);
}

class Message {
  final String id;
  final String author;
  final String text;
  final String? imageId;
  const Message(this.id, this.author, this.text, this.imageId);
  factory Message.fromJson(Map<String, dynamic> j) {
    final data = j['data'] as Map<String, dynamic>;
    return Message(
      j['id'].toString(),
      j['from'] as String,
      (data['Text'] as Map?)?['text'] as String? ?? '',
      (data['Image'] as Map?)?['link'] as String?,
    );
  }
}

class MessagePage {
  final List<Message> messages;
  final String? next;
  const MessagePage(this.messages, this.next);
}

enum Delivery { sending, sent, uncertain }

class Outgoing {
  final String localId;
  final String text;
  final Delivery status;
  final String? serverId;
  const Outgoing(this.localId, this.text, this.status, [this.serverId]);
}
