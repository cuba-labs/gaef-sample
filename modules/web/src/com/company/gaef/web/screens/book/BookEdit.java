package com.company.gaef.web.screens.book;

import com.haulmont.cuba.gui.screen.*;
import com.company.gaef.entity.Book;

@UiController("gaef$Book.edit")
@UiDescriptor("book-edit.xml")
@EditedEntityContainer("bookDc")
@LoadDataBeforeShow
public class BookEdit extends StandardEditor<Book> {
}
