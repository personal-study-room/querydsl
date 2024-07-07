package study.querydsl;


import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = AutowireMode.ALL)
@RequiredArgsConstructor
public class QuerydslBasicTest {

  private final EntityManager entityManager;
  JPAQueryFactory queryFactory;

  @BeforeEach
  void setUp() {
    queryFactory = new JPAQueryFactory(entityManager);

    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");

    entityManager.persist(teamA);
    entityManager.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);

    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);
    entityManager.persist(member1);
    entityManager.persist(member2);
    entityManager.persist(member3);
    entityManager.persist(member4);

    entityManager.flush();
    entityManager.clear();

    System.out.println("==============================================");
  }


  @Test
  void startJPQL() {
    // member1을 찾아라
    String qlString =
        "select m from Member m "
            + "where m.username = :username";
    Member findMember = entityManager.createQuery(
            qlString, Member.class)
        .setParameter("username", "member1")
        .getSingleResult();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  void startQuerydsl() {
    Member findMember = queryFactory
        .select(member)
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  void search() {
    Member findMember = queryFactory
        .select(member)
        .from(member)
        .where(
            member.username.eq("member1")
                .and(member.age.eq(10))
        )
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  void searchAndParam() {
    Member findMember = queryFactory
        .select(member)
        .from(member)
        .where(
            member.username.eq("member1"),
            member.age.eq(10)
        )
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }
}
