/// Код ошибки чтения контактов.
///
/// В интерфейсе код превращается в локализованный текст. Сам текст не
/// хранится в контроллере, чтобы не смешивать бизнес-логику и язык.
enum ContactsErrorKind {
  permissionCheckFailed,
  loadFailed,
  detailsFailed,
  detailsNotFound,
}
