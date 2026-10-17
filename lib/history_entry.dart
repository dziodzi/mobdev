class HistoryEntry {
  const HistoryEntry(this.expression, this.result);
  final String expression;
  final String result;

  Map<String, Object> toJson() => {'expression': expression, 'result': result};

  factory HistoryEntry.fromJson(Map<String, dynamic> json) => HistoryEntry(
        json['expression'] as String,
        json['result'] as String,
      );
}
