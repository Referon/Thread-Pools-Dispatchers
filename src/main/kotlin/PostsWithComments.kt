data class PostsWithComments(
    val post: Post,
    val author: Author,
    val comments: List<CommentsWithAuthor>
)