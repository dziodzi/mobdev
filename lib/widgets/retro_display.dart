import 'package:flutter/material.dart';

import '../theme/retro_theme.dart';

class RetroDisplay extends StatelessWidget {
  final String expression;
  final String result;
  final String? errorMessage;
  final String? memoryText;

  const RetroDisplay({
    super.key,
    required this.expression,
    required this.result,
    this.errorMessage,
    this.memoryText,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(10, 9, 10, 7),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(5),
        decoration: BoxDecoration(
          gradient: RetroPalette.shellGradient,
          borderRadius: BorderRadius.circular(11),
          border: Border.all(
            color: const Color(0xFF747B82),
            width: 1.2,
          ),
          boxShadow: const [
            BoxShadow(
              color: Color(0xA8000000),
              offset: Offset(0, 3),
              blurRadius: 4,
            ),
          ],
        ),
        child: Container(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 9),
          decoration: BoxDecoration(
            gradient: RetroPalette.lcdGradient,
            borderRadius: BorderRadius.circular(5),
            border: Border.all(
              color: RetroPalette.lcdBorder,
              width: 1.5,
            ),
            boxShadow: const [
              BoxShadow(
                color: Color(0x55000000),
                offset: Offset(0, 1),
                blurRadius: 1,
              ),
            ],
          ),
          child: LayoutBuilder(
            builder: (context, constraints) {
              final compact = constraints.maxHeight < 130;
              final expressionSize = compact ? 24.0 : 32.0;
              final resultSize = compact ? 18.0 : 22.0;

              return Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Row(
                    children: [
                      Text(
                        memoryText ?? '',
                        style: const TextStyle(
                          color: RetroPalette.lcdMuted,
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                          fontFamily: 'monospace',
                        ),
                      ),
                      const Spacer(),
                      Text(
                        errorMessage == null ? 'READY' : 'ERROR',
                        style: TextStyle(
                          color: errorMessage == null
                              ? RetroPalette.lcdMuted
                              : RetroPalette.red,
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                          fontFamily: 'monospace',
                          letterSpacing: 0.8,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 3),
                  Expanded(
                    child: Align(
                      alignment: Alignment.centerRight,
                      child: FittedBox(
                        fit: BoxFit.scaleDown,
                        alignment: Alignment.centerRight,
                        child: Text(
                          expression.isEmpty ? '0' : expression,
                          maxLines: 1,
                          style: TextStyle(
                            color: errorMessage == null
                                ? RetroPalette.lcdText
                                : RetroPalette.red,
                            fontSize: expressionSize,
                            fontWeight: FontWeight.w600,
                            fontFamily: 'monospace',
                            letterSpacing: 0.3,
                          ),
                        ),
                      ),
                    ),
                  ),
                  if (errorMessage != null)
                    Align(
                      alignment: Alignment.centerRight,
                      child: Text(
                        errorMessage!,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        textAlign: TextAlign.right,
                        style: const TextStyle(
                          color: RetroPalette.red,
                          fontSize: 10,
                          fontFamily: 'monospace',
                        ),
                      ),
                    ),
                  Row(
                    children: [
                      const Text(
                        'RESULT',
                        style: TextStyle(
                          color: RetroPalette.lcdMuted,
                          fontSize: 9,
                          fontWeight: FontWeight.w700,
                          fontFamily: 'monospace',
                          letterSpacing: 0.8,
                        ),
                      ),
                      const Spacer(),
                      Flexible(
                        child: FittedBox(
                          fit: BoxFit.scaleDown,
                          alignment: Alignment.centerRight,
                          child: Text(
                            result,
                            maxLines: 1,
                            style: TextStyle(
                              color: RetroPalette.lcdText,
                              fontSize: resultSize,
                              fontWeight: FontWeight.w700,
                              fontFamily: 'monospace',
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}
