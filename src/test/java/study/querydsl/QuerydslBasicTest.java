package study.querydsl;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
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
    }


    // JPQL
    @Test
    public void startJPQL() {
        //member1 찾기
        String qlString =
                "select m from Member m " +
                "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    // querydsl - 컴파일 시점에 오류를 잡아준다.
    @Test
    public void startQuerydsl() {


        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }



    // 이 것을ㄷ ㅓ선호 !
    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }



    @Test
    public void resultFetch(){
//        // 리스트 조회
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        //단건 조회
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        // limit 1
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchFirst();


//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//
//        results.getTotal();
//        List<Member> content = results.getResults(); // getResults()로 데이터가 나옴


        // count만 가져오기
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }


    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));


        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }



    @Test
    public void pageing1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 앞에 몇번째부터 스킵할꺼니 - 0부터 시작임.
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }


    // 전체 조회
    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 앞에 몇번째부터 스킵할꺼니 - 0부터 시작임.
                .limit(2)
                .fetchResults();


        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }


    // 집합 - 실무에서는 tuple 사용 잘 x, dto로 뽑아서 자주 씀.
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);  // select를 여러개 원하는거 꺼내면 tuple로 조회하게 됨.
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     * @throws Exception
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }



    // *****실무에서 많이 사용 - setter
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for(MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }



    // *****필드
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for(MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for(MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }





    // *** 동적 쿼리
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer agePararm = 10;

        List<Member> result = searchMember1(usernameParam, agePararm);
        assertThat(result.size()).isEqualTo(1);


    }

    private List<Member> searchMember1(String usernameCon, Integer ageCon) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCon != null) {
            builder.and(member.username.eq(usernameCon));
        }

        if (ageCon != null) {
            builder.and(member.age.eq(ageCon));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }






    // ***** 동적 쿼리 - where 다중 파라미터 사용
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer agePararm = 10;

        List<Member> result = searchMember2(usernameParam, agePararm);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember2(String usernameCon, Integer ageCon) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCon), ageEq(ageCon))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCon) {
        return usernameCon != null ? member.username.eq(usernameCon) : null;
    }

    private BooleanExpression ageEq(Integer ageCon) {
        return ageCon != null ? member.age.eq(ageCon) : null;
    }

    private BooleanExpression allEq(String usernameCon, Integer ageCon) {
        return usernameEq(usernameCon).and(ageEq(ageCon));
    }



    // 수정 벌크 - 문제접 : 영속성 콘텍스트는 1차캐쉬 무시하고 바로 db로 들어감 , 즉 db와 영속성컨텍스트 상태가 안맞음 , 영속성 컨텍스트가 항상 우선권을 가짐
    @Test
    @Commit
    public void bulkUpdate() {
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        //문제점 해결  - 초기화
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
    }


    // *****벌크 연산
    @Test
    public void bulkAdd(){
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

}
