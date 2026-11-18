package com.back.global.initData;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import com.back.domain.post.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BaseInitDataTest {
    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("test 프로파일에서는 샘플 데이터가 자동 생성되지 않는다")
    void t1() {
        assertThat(postRepository.count()).isZero();
    }

    @Test
    @DisplayName("빈 DB에서 샘플 데이터는 게시글 3건, 댓글 5건(3/2/0)이고 두 번 실행해도 중복되지 않는다")
    void t2() {
        BaseInitData baseInitData = new BaseInitData(postService);

        baseInitData.createSampleData();
        postService.flush();
        baseInitData.createSampleData();
        postService.flush();

        List<Post> posts = postRepository.findAll();
        assertThat(posts).hasSize(3);
        assertThat(posts).extracting(post -> post.getComments().size())
                .containsExactly(3, 2, 0);
    }
}
