class Session {
  const Session(this.user, this.token);

  final String user;
  final String token;
}

sealed class MessageContent {
  const MessageContent();
}

class TextContent extends MessageContent {
  const TextContent(this.text);
  final String text;
}

class PhotoContent extends MessageContent {
  const PhotoContent(this.path);
  final String path;
}

class Message {
  const Message(this.id, this.author, this.channel, this.time, this.content);

  factory Message.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    final MessageContent content;
    if (data['Text'] case {'text': final String text}) {
      content = TextContent(text);
    } else if (data['Image'] case {'link': final String path}
        when path.isNotEmpty) {
      content = PhotoContent(path);
    } else {
      throw const FormatException('Message content');
    }
    final timestamp = int.parse(json['time'].toString());
    return Message(
      int.parse(json['id'].toString()),
      json['from'] as String,
      json['to'] as String,
      DateTime.fromMillisecondsSinceEpoch(
        timestamp.abs() < 100000000000 ? timestamp * 1000 : timestamp,
      ),
      content,
    );
  }

  final int id;
  final String author;
  final String channel;
  final DateTime time;
  final MessageContent content;
}

enum Problem {
  credentials,
  unauthorized,
  connection,
  server,
  rejected,
  storage,
}

enum Delivery { sending, uncertain, rejected }

class Outgoing {
  const Outgoing(this.id, this.text, this.time, this.delivery, [this.problem]);

  factory Outgoing.fromJson(Map<String, dynamic> json) => Outgoing(
    json['id'] as String,
    json['text'] as String,
    DateTime.parse(json['time'] as String),
    json['rejected'] == true ? Delivery.rejected : Delivery.uncertain,
    Problem.values.where((value) => value.name == json['problem']).firstOrNull,
  );

  final String id;
  final String text;
  final DateTime time;
  final Delivery delivery;
  final Problem? problem;

  Outgoing withDelivery(Delivery status, [Problem? problem]) =>
      Outgoing(id, text, time, status, problem);

  Map<String, dynamic> toJson() => {
    'id': id,
    'text': text,
    'time': time.toIso8601String(),
    'rejected': delivery == Delivery.rejected,
    'problem': problem?.name,
  };
}
