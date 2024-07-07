package study.querydsl.entity;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Transactional
@TestConstructor(autowireMode = AutowireMode.ALL)
@RequiredArgsConstructor
class MemberTest {

  private final EntityManager entityManager;

  @Test
  void testEntity() {
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

    List<Member> members = entityManager.createQuery("select m from Member m",
        Member.class).getResultList();

    for (Member member : members) {
      System.out.println("member = " + member);
      System.out.println("member.getTeam() = " + member.getTeam());
    }

  }
}