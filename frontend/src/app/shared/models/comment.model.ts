export interface Comment {
  id: string;
  taskId: string;
  authorId: string;
  authorName: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface AddCommentRequest {
  content: string;
}

export interface EditCommentRequest {
  content: string;
}
