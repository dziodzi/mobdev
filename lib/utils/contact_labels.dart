import 'package:devicecontacts/l10n/app_localizations.dart';
import 'package:devicecontacts/models/contacts_error.dart';
import 'package:devicecontacts/models/contact_access_state.dart';

/// Убирает служебные маркеры Apple вида `_$!<Mother>!$_`.
String? cleanCustomLabel(String? raw) {
  if (raw == null) {
    return null;
  }
  var value = raw.trim();
  if (value.isEmpty) {
    return null;
  }
  final wrapped = RegExp(r'^_\$!<(.+)>!\$_$').firstMatch(value);
  if (wrapped != null) {
    value = wrapped.group(1)!.trim();
  }
  return value.isEmpty ? null : value;
}

/// Локализованное название типа телефона, email, адреса и других полей.
String localizedContactLabel(
  AppLocalizations l10n,
  String labelKey,
  String? customLabel,
) {
  final custom = cleanCustomLabel(customLabel);
  if (labelKey == 'custom') {
    return custom ?? l10n.labelCustom;
  }
  return _knownLabel(l10n, labelKey) ?? custom ?? l10n.labelOther;
}

String localizedPermissionMessage(
  AppLocalizations l10n,
  ContactAccessStatus status,
) {
  return switch (status) {
    ContactAccessStatus.denied => l10n.permissionDeniedMessage,
    ContactAccessStatus.permanentlyDenied =>
      l10n.permissionPermanentlyDeniedMessage,
    ContactAccessStatus.restricted => l10n.permissionRestrictedMessage,
    ContactAccessStatus.notDetermined => l10n.permissionNotDeterminedMessage,
    ContactAccessStatus.limited => l10n.limitedAccessBanner,
    ContactAccessStatus.granted => l10n.permissionExplanation,
    ContactAccessStatus.unknown => l10n.permissionUnknownMessage,
  };
}

String localizedErrorMessage(AppLocalizations l10n, ContactsErrorKind kind) {
  return switch (kind) {
    ContactsErrorKind.permissionCheckFailed => l10n.errorPermissionCheck,
    ContactsErrorKind.loadFailed => l10n.errorLoadContacts,
    ContactsErrorKind.detailsFailed => l10n.errorLoadDetails,
    ContactsErrorKind.detailsNotFound => l10n.errorContactNotFound,
  };
}

String? _knownLabel(AppLocalizations l10n, String labelKey) {
  return switch (labelKey) {
    'mobile' => l10n.labelMobile,
    'home' => l10n.labelHome,
    'work' => l10n.labelWork,
    'main' => l10n.labelMain,
    'other' => l10n.labelOther,
    'school' => l10n.labelSchool,
    'iPhone' => l10n.labelIPhone,
    'iCloud' => l10n.labelICloud,
    'pager' => l10n.labelPager,
    'homeFax' => l10n.labelHomeFax,
    'workFax' => l10n.labelWorkFax,
    'otherFax' => l10n.labelOtherFax,
    'workMobile' => l10n.labelWorkMobile,
    'workPager' => l10n.labelWorkPager,
    'companyMain' => l10n.labelCompanyMain,
    'assistant' => l10n.labelAssistant,
    'car' => l10n.labelCar,
    'callback' => l10n.labelCallback,
    'radio' => l10n.labelRadio,
    'isdn' => l10n.labelIsdn,
    'mms' => l10n.labelMms,
    'telex' => l10n.labelTelex,
    'ttyTdd' => l10n.labelTtyTdd,
    'appleWatch' => l10n.labelAppleWatch,
    'birthday' => l10n.labelBirthday,
    'anniversary' => l10n.labelAnniversary,
    'homepage' => l10n.labelHomepage,
    'blog' => l10n.labelBlog,
    'profile' => l10n.labelProfile,
    'ftp' => l10n.labelFtp,
    'brother' => l10n.labelBrother,
    'sister' => l10n.labelSister,
    'mother' => l10n.labelMother,
    'father' => l10n.labelFather,
    'parent' => l10n.labelParent,
    'child' => l10n.labelChild,
    'spouse' => l10n.labelSpouse,
    'partner' => l10n.labelPartner,
    'friend' => l10n.labelFriend,
    'manager' => l10n.labelManager,
    'relative' => l10n.labelRelative,
    'domesticPartner' => l10n.labelDomesticPartner,
    'referredBy' => l10n.labelReferredBy,
    'aunt' => l10n.labelAunt,
    'boyfriend' => l10n.labelBoyfriend,
    'brotherInLaw' => l10n.labelBrotherInLaw,
    'childInLaw' => l10n.labelChildInLaw,
    'colleague' => l10n.labelColleague,
    'cousin' => l10n.labelCousin,
    'daughter' => l10n.labelDaughter,
    'fatherInLaw' => l10n.labelFatherInLaw,
    'girlfriend' => l10n.labelGirlfriend,
    'grandchild' => l10n.labelGrandchild,
    'granddaughter' => l10n.labelGranddaughter,
    'grandfather' => l10n.labelGrandfather,
    'grandmother' => l10n.labelGrandmother,
    'grandparent' => l10n.labelGrandparent,
    'grandson' => l10n.labelGrandson,
    'husband' => l10n.labelHusband,
    'motherInLaw' => l10n.labelMotherInLaw,
    'nephew' => l10n.labelNephew,
    'niece' => l10n.labelNiece,
    'parentInLaw' => l10n.labelParentInLaw,
    'sibling' => l10n.labelSibling,
    'siblingInLaw' => l10n.labelSiblingInLaw,
    'sisterInLaw' => l10n.labelSisterInLaw,
    'son' => l10n.labelSon,
    'stepbrother' => l10n.labelStepbrother,
    'stepchild' => l10n.labelStepchild,
    'stepdaughter' => l10n.labelStepdaughter,
    'stepfather' => l10n.labelStepfather,
    'stepmother' => l10n.labelStepmother,
    'stepparent' => l10n.labelStepparent,
    'stepsister' => l10n.labelStepsister,
    'stepson' => l10n.labelStepson,
    'teacher' => l10n.labelTeacher,
    'uncle' => l10n.labelUncle,
    'wife' => l10n.labelWife,
    'aim' => l10n.socialAim,
    'facebook' => l10n.socialFacebook,
    'flickr' => l10n.socialFlickr,
    'instagram' => l10n.socialInstagram,
    'jabber' => l10n.socialJabber,
    'line' => l10n.socialLine,
    'linkedIn' => l10n.socialLinkedIn,
    'messenger' => l10n.socialMessenger,
    'msn' => l10n.socialMsn,
    'mySpace' => l10n.socialMySpace,
    'pinterest' => l10n.socialPinterest,
    'qq' => l10n.socialQq,
    'skype' => l10n.socialSkype,
    'snapchat' => l10n.socialSnapchat,
    'telegram' => l10n.socialTelegram,
    'tumblr' => l10n.socialTumblr,
    'twitter' => l10n.socialTwitter,
    'viber' => l10n.socialViber,
    'wechat' => l10n.socialWechat,
    'whatsapp' => l10n.socialWhatsapp,
    'yahoo' => l10n.socialYahoo,
    'youtube' => l10n.socialYoutube,
    'discord' => l10n.socialDiscord,
    'tiktok' => l10n.socialTiktok,
    'gameCenter' => l10n.socialGameCenter,
    'googleTalk' => l10n.socialGoogleTalk,
    'icq' => l10n.socialIcq,
    'sinaWeibo' => l10n.socialSinaWeibo,
    'tencentWeibo' => l10n.socialTencentWeibo,
    'gaduGadu' => l10n.socialGaduGadu,
    'baiduTieba' => l10n.socialBaiduTieba,
    'netmeeting' => l10n.socialNetmeeting,
    'yelp' => l10n.socialYelp,
    'medium' => l10n.socialMedium,
    _ => null,
  };
}
