package study.querydsl;


import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
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

  @Test
  void aggregation() {
    List<Tuple> result = queryFactory
        .select(
            member.count(),
            member.age.sum(),
            member.age.avg(),
            member.age.min()
        )
        .from(member)
        .fetch();

    Tuple tuple = result.get(0);
    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.min())).isEqualTo(10);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);
  }


  // 팀의 이름과 각 팀의 평균 연력을 구해라
  @Test
  void group() {
    List<Tuple> result = queryFactory
        .select(team.teamName, member.age.avg())
        .from(member)
        .join(member.team, team)
        .groupBy(team.teamName)
        .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.teamName)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15);
    assertThat(teamB.get(team.teamName)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);
  }


  // teamA 에 소속된 모든 회원
  @Test
  void join() {
    List<Member> result = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.teamName.eq("teamA"))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("member1", "member2");
  }


  // 회원의 이름이 팀 이름과 같은 회원 조회
  @Test
  void thetaJoin() {
    entityManager.persist(new Member("teamA"));
    entityManager.persist(new Member("teamB"));

    List<Member> result = queryFactory
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.teamName))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("teamA", "teamB");
  }

  @Test
  void thetaJoinCount() {
    List<Member> result = queryFactory
        .select(member)
        .from(member, team)
        .fetch();

    System.out.println("result.size() = " + result.size()); // 4 =>
    for (Member member1 : result) {
      System.out.println("member1.getUsername() = " + member1.getUsername());
      System.out.println("member1.getTeam().getTeamName() = " + member1.getTeam().getTeamName());
    }
    /**
     * member1.getUsername() = member1
     * member1.getTeam().getTeamName() = teamA
     * member1.getUsername() = member2
     * member1.getTeam().getTeamName() = teamA
     * member1.getUsername() = member3
     * member1.getTeam().getTeamName() = teamB
     * member1.getUsername() = member4
     * member1.getTeam().getTeamName() = teamB
     *
     * 알아서 left join이 되었음.
     */
  }


  @Test
  void thetaJoinCount2() {
    List<Member> result = queryFactory
        .selectFrom(member)
        .join(team)
        .on(member.team.eq(team))
        .fetch();

    System.out.println("result.size() = " + result.size()); // 4 =>
    for (Member member1 : result) {
      System.out.println("member1.getUsername() = " + member1.getUsername());
      System.out.println("member1.getTeam().getTeamName() = " + member1.getTeam().getTeamName());
    }

    /**
     * Hibernate:
     *     select
     *         m1_0.member_id,
     *         m1_0.age,
     *         m1_0.team_id,
     *         m1_0.username
     *     from
     *         member m1_0
     *     join
     *         team t1_0
     *             on m1_0.team_id=t1_0.team_id
     */
  }

  // 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
  @Test
  void join_on_filtering() {
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        .on(team.teamName.eq("teamA"))
        .fetch();

    // left join 이므로 4개
    assertThat(result.size()).isEqualTo(4);

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

    /**
     * tuple = [Member(id=1, username=member1, age=10), Team(id=1, teamName=teamA)]
     * tuple = [Member(id=2, username=member2, age=20), Team(id=1, teamName=teamA)]
     * tuple = [Member(id=3, username=member3, age=30), null]
     * tuple = [Member(id=4, username=member4, age=40), null]
     */
  }

  @Test
  void join_on_filtering2() {
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .join(member.team, team)
        .on(team.teamName.eq("teamA"))
        .fetch();

    // left join 이므로 4개
    assertThat(result.size()).isEqualTo(2);

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

    /**
     * tuple = [Member(id=1, username=member1, age=10), Team(id=1, teamName=teamA)]
     * tuple = [Member(id=2, username=member2, age=20), Team(id=1, teamName=teamA)]
     */
  }


  @Test
  void join_on_filtering3() {
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        .where(team.teamName.eq("teamA"))
        .fetch();

    // left join 이므로 4개
    assertThat(result.size()).isEqualTo(2);

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

    /**
     * tuple = [Member(id=1, username=member1, age=10), Team(id=1, teamName=teamA)]
     * tuple = [Member(id=2, username=member2, age=20), Team(id=1, teamName=teamA)]
     */
  }


  /**
   * 2. 연관관계 없는 엔티티 외부 조인
   * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
   * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
   * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name */
  @Test
  public void join_on_no_relation() throws Exception {
    entityManager.persist(new Member("teamA"));
    entityManager.persist(new Member("teamB"));
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(team).on(member.username.eq(team.teamName))
        .fetch();
    for (Tuple tuple : result) {
      System.out.println("t=" + tuple);
    }

    /**
     * t=[Member(id=1, username=member1, age=10), null]
     * t=[Member(id=2, username=member2, age=20), null]
     * t=[Member(id=3, username=member3, age=30), null]
     * t=[Member(id=4, username=member4, age=40), null]
     * t=[Member(id=5, username=teamA, age=0), Team(id=1, teamName=teamA)]
     * t=[Member(id=6, username=teamB, age=0), Team(id=2, teamName=teamB)]
     */
  }

  private final EntityManagerFactory emf;

  @Test
  void fetchJoinNo() {
    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as(" 페치 조인 미적용").isFalse();

  }

  @Test
  void fetchJoinUse() {
    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(member.username.eq("member1")).fetchJoin()
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as(" 페치 조인 적용").isTrue();

  }
}
