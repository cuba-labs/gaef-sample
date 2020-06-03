package com.company.gaef.web.screens.book;

import com.haulmont.cuba.gui.screen.*;
import com.company.gaef.entity.Book;

@UiController("gaef$Book.browse")
@UiDescriptor("book-browse.xml")
@LookupComponent("booksTable")
@LoadDataBeforeShow
public class BookBrowse extends StandardLookup<Book> {
}
