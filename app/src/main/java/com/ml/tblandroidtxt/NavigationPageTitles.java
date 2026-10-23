package com.ml.tblandroidtxt;

/** User-facing page titles kept separate from the internal navigation keys. */
final class NavigationPageTitles {
    private NavigationPageTitles() {}

    static String title(String page) {
        if ("Translate".equals(page)) return "Dịch";
        if ("Jobs".equals(page)) return "Công việc";
        if ("JobsHistory".equals(page)) return "Công việc · Lịch sử";
        if ("Editorial".equals(page)) return EditorialApiPresenter.TAB_TITLE;
        if ("Settings".equals(page)) return "Cài đặt";
        if ("Files".equals(page)) return "Thư viện";
        if ("RAW".equals(page)) return "Thư viện · RAW";
        if ("Glossaries".equals(page)) return "Thư viện · Glossary";
        if ("Pronouns".equals(page)) return "Thư viện · Pronoun";
        if ("ChapterSets".equals(page)) return "Thư viện · Tập";
        if ("Sample".equals(page)) return "Thư viện · Mẫu";
        return "Dịch";
    }
}
