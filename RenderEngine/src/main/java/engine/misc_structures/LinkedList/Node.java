package engine.misc_structures.LinkedList;

public class Node<T> {

    public T data = null;
    public Node<T> previous = null;
    public Node<T> next = null;

    Node(T data,Node<T> prev, Node<T> next) {
        this.data = data;
        this.previous = prev;
        this.next = next;
    }

    Node() {}

    Node(T data) {
        this.data = data;
    }

}
