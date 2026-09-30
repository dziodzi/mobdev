import 'package:flutter/material.dart';

import '../theme/retro_theme.dart';

class RetroHeader extends StatelessWidget {
  final VoidCallback onHistory;

  const RetroHeader({
    super.key,
    required this.onHistory,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 44,
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFF4B5158), Color(0xFF23272C)],
        ),
        border: Border(
          bottom: BorderSide(
            color: RetroPalette.cyanDark,
            width: 1.5,
          ),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10),
        child: Row(
          children: [
            Container(
              width: 7,
              height: 7,
              decoration: const BoxDecoration(
                color: RetroPalette.cyan,
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: Color(0x8873C5D2),
                    blurRadius: 5,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            const Text(
              'ADVANCED CALCULATOR',
              style: TextStyle(
                color: Color(0xFFF2F4F5),
                fontSize: 13,
                fontWeight: FontWeight.w700,
                letterSpacing: 1.0,
                fontFamily: 'monospace',
              ),
            ),
            const Spacer(),
            GestureDetector(
              onTap: onHistory,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 8,
                  vertical: 5,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFF252A2F),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(
                    color: const Color(0xFF697179),
                    width: 1,
                  ),
                ),
                child: const Text(
                  'HISTORY',
                  style: TextStyle(
                    color: Color(0xFFE9F7F8),
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.7,
                    fontFamily: 'monospace',
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
