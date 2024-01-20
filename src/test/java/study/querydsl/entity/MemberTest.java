package study.querydsl.entity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.lang.model.SourceVersion;
import java.sql.SQLOutput;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // 기본적으로 rollback 해줌 테스트에서.
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    public void testEntity() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);


        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);


        // 초기화
        em.flush(); // 영속성 컨텍스트에 있는 object를 쿼리로 만들어서 실제  db에 날림
        em.clear(); // 영속성 컨텍스트에 있는 완전 초기화해서 캐쉬가 다 날라감

        //확인
        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        // 원래 테스트할때는 assert로 해야 한다.
        for (Member member :members) {
            System.out.println("member = " + member);
            System.out.println("-> member.team  " + member.getTeam());

        }

    }



}