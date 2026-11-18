package com.back.global.initData;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발 DB가 비어 있을 때만 샘플 데이터(게시글 3건, 댓글 5건)를 생성한다.
 * 게시글이 하나라도 있으면 아무것도 하지 않으므로 재시작해도 중복 생성되지 않는다.
 * 테스트(test 프로파일)에서는 실행되지 않는다.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class BaseInitData implements ApplicationRunner {
    private final PostService postService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createSampleData();
    }

    @Transactional
    public void createSampleData() {
        if (postService.count() > 0) return;

        Post post1 = postService.write("제목 1", "내용 1");
        Post post2 = postService.write("제목 2", "내용 2");
        postService.write("제목 3", "내용 3");

        postService.writeComment(post1, "댓글 1-1");
        postService.writeComment(post1, "댓글 1-2");
        postService.writeComment(post1, "댓글 1-3");
        postService.writeComment(post2, "댓글 2-1");
        postService.writeComment(post2, "댓글 2-2");
    }
}
