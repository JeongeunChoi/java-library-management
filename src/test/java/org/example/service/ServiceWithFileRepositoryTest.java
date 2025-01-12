package org.example.service;

import org.example.domain.Book;
import org.example.domain.BookStatusType;
import org.example.exception.ExceptionCode;
import org.example.repository.FileRepository;
import org.example.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceWithFileRepositoryTest {
    private Repository repository;
    private LibraryManagementService libraryManagementService;
    private Integer nextBookId;
    private String bookInfoTestCsvPath = "src/test/resources/bookInfoTest.csv";

    @BeforeEach
    void beforeEach() {
        File file = new File(bookInfoTestCsvPath);
        if (file.exists()) file.delete();
        repository = new FileRepository(bookInfoTestCsvPath);
        libraryManagementService = new LibraryManagementService(repository);
        nextBookId = repository.getNextBookId();
    }

    @Test
    @DisplayName("도서 등록 성공")
    void registerBook_Success() {
        //given

        //when
        for (int i = 1; i <= 6; i++) {
            libraryManagementService.registerBook("testTitle", "testAuthor", 123);
        }

        //then
        assertEquals(repository.findAllBooks().size(), 6);
    }

    @Test
    @DisplayName("전체 도서 목록 조회 성공")
    void searchBooks_Success() {
        //given
        for (int i = 1; i <= 6; i++) {
            repository.save(createTestBook());
        }

        //when
        List<Book> books = libraryManagementService.searchAllBooks();

        //then
        assertEquals(repository.findAllBooks().size(), 6);
    }

    @Test
    @DisplayName("제목으로 도서 검색 성공")
    void searchBookByTitle_Success() {
        //given
        repository.save(createTestBookWithTitle("abc"));
        repository.save(createTestBookWithTitle("cka"));
        repository.save(createTestBookWithTitle("def"));
        repository.save(createTestBookWithTitle("ghi"));

        //when
        List<Book> books = libraryManagementService.searchBookBy("a");

        //then
        assertEquals(books.size(), 2);
    }

    @Test
    @DisplayName("도서 대여 성공")
    void borrowBook_Success() {
        //given
        repository.save(createTestBook());

        //when
        libraryManagementService.borrowBook(1);

        //then
        assertEquals(repository.findBookById(1).get().getStatus(), BookStatusType.BORROWING);
    }

    @Test
    @DisplayName("도서 대여 실패 - 대여중인 도서인 경우")
    void borrowBook_Fail_Borrowing() {
        //given
        repository.save(createTestBookWithStatus(BookStatusType.BORROWING));

        //when, then
        try {
            libraryManagementService.borrowBook(1);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), ExceptionCode.ALREADY_BORROWED.getMessage());
        }
    }

    @Test
    @DisplayName("도서 대여 실패 - 정리중인 도서인 경우")
    void borrowBook_Fail_Organizing() {
        //given
        repository.save(createTestBookWithStatus(BookStatusType.ORGANIZING));

        //when, then
        try {
            libraryManagementService.borrowBook(1);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), ExceptionCode.BEING_ORGANIZED.getMessage());
        }
    }

    @Test
    @DisplayName("도서 대여 실패 - 분실된 도서인 경우")
    void borrowBook_Fail_Lost() {
        //given
        repository.save(createTestBookWithStatus(BookStatusType.LOST));

        //when, then
        try {
            libraryManagementService.borrowBook(1);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), ExceptionCode.LOST.getMessage());
        }
    }

    @Test
    @DisplayName("도서 반납 성공")
    void returnBook_Success() {
        //given
        repository.save(createTestBookWithStatus(BookStatusType.BORROWING));

        //when
        libraryManagementService.returnBook(1);

        //then
        assertEquals(repository.findBookById(1).get().getStatus(), BookStatusType.ORGANIZING);
    }

    @Test
    @DisplayName("도서 반납 실패 - 원래 대여가 가능한 도서인 경우")
    void returnBook_Fail_BorrrowAvailable() {
        //given
        repository.save(createTestBookWithStatus(BookStatusType.BORROW_AVAILABE));

        //when, then
        try {
            libraryManagementService.returnBook(1);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), ExceptionCode.AVAILABLE_FOR_BORROW.getMessage());
        }
    }

    @Test
    @DisplayName("도서 분실 성공")
    void lostBook_Success() {
        //given
        repository.save(createTestBook());

        //when
        libraryManagementService.lostBook(1);

        //then
        assertEquals(repository.findBookById(1).get().getStatus(), BookStatusType.LOST);
    }

    @Test
    @DisplayName("도서 분실 실패 - 이미 분실 처리된 도서인 경우")
    void lostBook_Fail_Lost() {
        //given
        repository.save(createTestBookWithStatus(BookStatusType.LOST));

        //when, then
        try {
            libraryManagementService.lostBook(1);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), ExceptionCode.LOST.getMessage());
        }
    }

    @Test
    @DisplayName("도서 삭제 성공")
    void deleteBook_Success() {
        //given
        repository.save(createTestBook());

        //when
        libraryManagementService.deleteBook(1);

        //then
        assertFalse(repository.findBookById(1).isPresent());
    }

    @Test
    @DisplayName("도서 삭제 실패 - 존재하지 않는 도서인 경우")
    void deleteBook_Fail_NotExist() {
        //given

        //when, then
        try {
            libraryManagementService.deleteBook(1);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), ExceptionCode.INVALID_BOOK.getMessage());
        }
    }

    private Book createTestBook() {
        return Book.createWithoutStatus(nextBookId++, "testTitle", "testAuthor", 123);
    }

    private Book createTestBookWithTitle(String title) {
        return Book.createWithoutStatus(nextBookId++, title, "testAuthor", 123);
    }

    private Book createTestBookWithStatus(BookStatusType status) {
        return Book.createWithStatus(nextBookId++, "testTitle", "testAuthor", 123, status);
    }
}