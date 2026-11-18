package com.back.domain.post.post.service;

import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.repository.PostRepository;
import com.back.domain.post.postComment.entity.PostComment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    public long count() {
        return postRepository.count();
    }

    public Post write(String title, String content) {
        return postRepository.save(new Post(title, content));
    }

    /** 게시글에 속한 댓글만 찾는다. 다른 글의 댓글 ID면 NoSuchElementException(404). */
    public PostComment findCommentById(Post post, long id) {
        return post.findCommentById(id).orElseThrow();
    }

    public PostComment writeComment(Post post, String content) {
        return post.addComment(content);
    }

    public List<Post> findAll() {
        return postRepository.findAllByOrderByIdDesc();
    }

    public Post findById(long id) {
        return postRepository.findById(id).orElseThrow();
    }

    public void modify(Post post, String title, String content) {
        post.modify(title, content);
    }

    public void delete(Post post) {
        postRepository.delete(post);
    }

    public void flush() {
        postRepository.flush();
    }
}
