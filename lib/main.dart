import 'package:flutter/material.dart';

import 'controllers/calculator_controller.dart';
import 'screens/calculator_screen.dart';
import 'services/storage_service.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final storage = SharedPreferencesStorageService();
  final controller = CalculatorController(storage: storage);
  await controller.load();
  runApp(AdvancedCalculatorApp(controller: controller));
}

class AdvancedCalculatorApp extends StatelessWidget {
  const AdvancedCalculatorApp({super.key, required this.controller});

  final CalculatorController controller;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Advanced Calculator',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF355C7D),
          brightness: Brightness.light,
        ),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF355C7D),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      home: CalculatorScreen(controller: controller),
    );
  }
}
