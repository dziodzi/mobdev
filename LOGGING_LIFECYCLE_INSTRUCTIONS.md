Добавить логирование жизненного цикла:

- Импортировать android.util.Log
- В MainActivity.kt добавить Log.d(...) в:
  onCreate, onStart, onResume, onPause, onStop, onDestroy, onRestart
- Также удобно добавить логирование при onSaveInstanceState и onRestoreInstanceState (или в onCreate, когда savedInstanceState != null).

Что логировать для “тумблера”:
- При включении/выключении "Не сохранять данные при смене конфигурации" (если есть) Android может:
  - либо сохранять Bundle через onSaveInstanceState и восстанавливать
  - либо не восстанавливать (state будет сброшен)
- Проверять по Log: вызывает ли система onSaveInstanceState и вызывается ли восстановление currentValue/firstValue/operation/isNewOperation.

Практика:
- В onCreate сразу после savedInstanceState?.let { ... } вывести Log.d с currentValue/firstValue/operation/isNewOperation.
- В onSaveInstanceState вывести Log.d со значениями перед put*.

