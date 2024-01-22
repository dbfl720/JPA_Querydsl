package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom { //~Impl 이름 꼭 붙여야됨. (규칙)

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    // ***** 동적 쿼리와 성능 최적화 조회 - where절 파라미터 사용 - 메서드를 재사용 할 수 있는 큰 장점
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        userNameEq(condition.getUsername()),
                        teanNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                        //isvalid() 추가 메서드 가능
                )
                .fetch(); // //  쿼리문을 처리 한 후, 반환되는 값들을 그대로 리스트로 가져옵니다. 이떄, 반환되는 데이터가 없으면 빈 리스트(Empty List)가 반환됩니다.
    }




    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        userNameEq(condition.getUsername()),
                        teanNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                        //isvalid() 추가 메서드 가능
                )
                .offset(pageable.getOffset()) // 몇번째를 skip하고 몇번째부터 시작할 꺼야?
                .limit(pageable.getPageSize()) // 한번 조회할때 몇개까지 조회할꺼야?
                .fetchResults();// 컨텐츠용 포함 & count 쿼리도 같이 실행됩니다

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);

    }


    //**** count쿼리를 최적화 하고 싶으면 따로 이렇게 분리해서 해랏! 데이터 너무 없는데 하면 안되고, 데이터 많으면 해랏  - command + option + m해서 분리해랏
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        userNameEq(condition.getUsername()),
                        teanNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                        //isvalid() 추가 메서드 가능
                )
                .offset(pageable.getOffset()) // 몇번째를 skip하고 몇번째부터 시작할 꺼야?
                .limit(pageable.getPageSize()) // 한번 조회할때 몇개까지 조회할꺼야?
                .fetch();// 컨텐츠용 포함 & count 쿼리도 같이 실행됩니다


        // ***** CountQuery 최적화
        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        userNameEq(condition.getUsername()),
                        teanNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                        //isvalid() 추가 메서드 가능
                );

        // ***** CountQuery 최적화
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }




    private BooleanExpression userNameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teanNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName): null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
