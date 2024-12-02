package com.ll.simpleDb;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class SimpleDbTest {
    private static SimpleDb simpleDb;

    @BeforeAll
    public static void beforeAll() {
        simpleDb = new SimpleDb("localhost", "root", "lldj123414", "simpleDb__test");
        simpleDb.setDevMode(true);

        createArticleTable();
    }

    @BeforeEach
    public void beforeEach() {
        truncateArticleTable();
        makeArticleTestData();
    }

    @AfterAll
    public static void afterAll() {
        simpleDb.close();
    }


    private static void createArticleTable() {
        simpleDb.run("DROP TABLE IF EXISTS article");

        simpleDb.run("""
                CREATE TABLE article (
                    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY(id),
                    createdDate DATETIME NOT NULL,
                    modifiedDate DATETIME NOT NULL,
                    title VARCHAR(100) NOT NULL,
                    `body` TEXT NOT NULL,
                    isBlind BIT(1) NOT NULL DEFAULT 0
                )
                """);
    }

    private void makeArticleTestData() {
        IntStream.rangeClosed(1, 6).forEach(no -> {
            boolean isBlind = no > 3;
            String title = "제목%d".formatted(no);
            String body = "내용%d".formatted(no);

            simpleDb.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
        });
    }

    private void truncateArticleTable() {
        simpleDb.run("TRUNCATE article");
    }

    @Test
    @DisplayName("데이터 베이스 연결 테스트 - BeforeAll, BeforeEach 로 확인")
    public void JdbcConnectTest() {

    }

    @Test
    @DisplayName("INSERT 테스트")
    public void insertTest() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        INSERT INTO article
        SET createdDate = NOW() ,
        modifiedDate = NOW() ,
        title = '제목 new' ,
        body = '내용 new'
        */
        sql.append("INSERT INTO article")
            .append("SET createdDate = NOW()")
            .append(", modifiedDate = NOW()")
            .append(", title = ?", "제목 new")
            .append(", body = ?", "내용 new");

        long newId = sql.insert(); // AUTO_INCREMENT 에 의해서 생성된 주키 리턴

        assertThat(newId).isGreaterThan(6);
    }

    @Test
    @DisplayName("UPDATE 테스트")
    public void updateTest() {
        Sql sql = simpleDb.genSql();

        // id가 0, 1, 2, 3인 글 수정
        // id가 0인 글은 없으니, 실제로는 3개의 글이 삭제됨

        /*
        == rawSql ==
        UPDATE article
        SET title = '제목 new'
        WHERE id IN ('0', '1', '2', '3')
        */
        sql.append("UPDATE article")
            .append("SET title = ?", "제목 new")
            .append("WHERE id IN (?, ?, ?, ?)", 0, 1, 2, 3);

        // 수정된 row 개수
        long affectedRowsCount = sql.update();

        assertThat(affectedRowsCount).isEqualTo(3);
    }

    @Test
    @DisplayName("DELETE 테스트")
    public void deleteTest() {
        Sql sql = simpleDb.genSql();

        // id가 0, 1, 3인 글 삭제
        // id가 0인 글은 없으니, 실제로는 2개의 글이 삭제됨
        /*
        == rawSql ==
        DELETE FROM article
        WHERE id IN ('0', '1', '3')
        */
        sql.append("DELETE")
            .append("FROM article")
            .append("WHERE id IN (?, ?, ?)", 0, 1, 3);

        // 삭제된 row 개수
        long affectedRowsCount = sql.delete();

        assertThat(affectedRowsCount).isEqualTo(2);
    }

    @Test
    @DisplayName("SELECT ROWS 테스트")
    public void selectRowsTest() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT *
        FROM article
        ORDER BY id ASC
        LIMIT 3
        */
        sql.append("SELECT * FROM article ORDER BY id ASC LIMIT 3");
        List<Map<String, Object>> articleRows = sql.selectRows();

        IntStream.range(0, articleRows.size()).forEach(i -> {
            long id = i + 1;

            Map<String, Object> articleRow = articleRows.get(i);

            assertThat(articleRow.get("id")).isEqualTo(id);
            assertThat(articleRow.get("title")).isEqualTo("제목%d".formatted(id));
            assertThat(articleRow.get("body")).isEqualTo("내용%d".formatted(id));
            assertThat(articleRow.get("createdDate")).isInstanceOf(LocalDateTime.class);
            assertThat(articleRow.get("createdDate")).isNotNull();
            assertThat(articleRow.get("modifiedDate")).isInstanceOf(LocalDateTime.class);
            assertThat(articleRow.get("modifiedDate")).isNotNull();
            assertThat(articleRow.get("isBlind")).isEqualTo(false);
        });
    }

    @Test
    @DisplayName("SELECT DateTime 테스트")
    public void selectDateTimeTest() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT NOW()
        */
        sql.append("SELECT NOW()");

        LocalDateTime datetime = sql.selectDatetime();

        long diff = ChronoUnit.SECONDS.between(datetime, LocalDateTime.now());

        assertThat(diff).isLessThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("SELECT ID 테스트")
    public void selectIdTest() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT id
        FROM article
        WHERE id = 1
        */
        sql.append("SELECT id")
            .append("FROM article")
            .append("WHERE id = 1");


        Long id = sql.selectLong();

        assertThat(id).isEqualTo(1);
    }

    @Test
    @DisplayName("SELECT STRING 테스트")
    public void selectStringTest() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT title
        FROM article
        WHERE id = 1
        */
        sql.append("SELECT title")
            .append("FROM article")
            .append("WHERE id = 1");

        String title = sql.selectString();

        assertThat(title).isEqualTo("제목1");
    }

    @Test
    @DisplayName("SELECT Boolean 테스트")
    public void selectBoolean() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT isBlind
        FROM article
        WHERE id = 1
        */
        sql.append("SELECT isBlind")
            .append("FROM article")
            .append("WHERE id = 1");

        Boolean isBlind = sql.selectBoolean();

        assertThat(isBlind).isEqualTo(false);
    }

    @Test
    @DisplayName("SELECT Boolean 테스트2")
    public void selectBoolean2() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT 1 = 1
        */
        sql.append("SELECT 1 = 1");

        Boolean isBlind = sql.selectBoolean();

        assertThat(isBlind).isEqualTo(true);
    }

    @Test
    @DisplayName("SELECT Boolean 테스트3")
    public void selectBoolean3() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT 1 = 0
        */
        sql.append("SELECT 1 = 0");

        Boolean isBlind = sql.selectBoolean();

        assertThat(isBlind).isEqualTo(false);
    }

    @Test
    @DisplayName("SELECT, LIKE 사용법")
    public void selectLikeTest() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT COUNT(*)
        FROM article
        WHERE id BETWEEN '1' AND '3'
        AND title LIKE CONCAT('%', '제목' '%')
        */
        sql.append("SELECT COUNT(*)")
            .append("FROM article")
            .append("WHERE id BETWEEN ? AND ?", 1, 3)
            .append("AND title LIKE CONCAT('%', ? '%')", "제목");

        long count = sql.selectLong();

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("appendIn 테스트")
    public void appendInTest() {
        Sql sql = simpleDb.genSql();
        /*
        == rawSql ==
        SELECT COUNT(*)
        FROM article
        WHERE id IN ('1', '2', '3')
        */
        sql.append("SELECT COUNT(*)")
            .append("FROM article")
            .appendIn("WHERE id IN (?)", 1, 2, 3);

        long count = sql.selectLong();

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("selectLongs, ORDER BY FIELD 사용법")
    public void t014() {
        Long[] ids = new Long[]{2L, 1L, 3L};

        Sql sql = simpleDb.genSql();
        /*
        SELECT id
        FROM article
        WHERE id IN ('2', '3', '1')
        ORDER BY FIELD (id, '2', '3', '1')
        */
        sql.append("SELECT id")
            .append("FROM article")
            .appendIn("WHERE id IN (?)", ids)
            .appendIn("ORDER BY FIELD (id, ?)", ids);

        List<Long> foundIds = sql.selectLongs();

        assertThat(foundIds).isEqualTo(Arrays.stream(ids).toList());
    }


}
