package models.DataStructure.LinkedList;

public class DoublyLinkedList<T> {

    public int size = 0;

    protected Node<T> prev = null;
    protected Node<T> current = null;
    protected Node<T> next = null;

    public Node<T> head = null;
    public Node<T> tail = null;

    public DoublyLinkedList(Node<T> initNode) {
        this.head = initNode;
        this.tail = head;
        this.head.next = tail;
        this.tail.previous = head;

        current = head;
        prev = current.previous;
        next = current.next;
        size++;
    }

    public DoublyLinkedList() {
        current = head;
    }

    public T peek() {
        if(current == null) {
            return null;
        }
        return current.data;

    }

    public T peekHead() {
        return head.data;
    }

    public T peekTail() {
        return tail.data;
    }

    public T peekNext() {
        if(current == null) {
            if(next != null) {
                current = next;
                next = current.next;
            }
            return null;
        }
        prev = current;
        next = current.next;
        current = current.next;

        return prev.data;
    }

    public T peekPrevious() {
        if(current == null) {
            if(prev != null) {
                current = prev;
                prev = current.previous;
            }
            return null;
        }
        next = current;
        prev = current.previous;
        current = current.previous;

        return next.data;
    }

    public void resetLoc() {
        current = head;
        next = current.next;
        prev = current.previous;
    }

    public T popHead() {
        if(head == null) {
            return null;
        }
        T ret = head.data;
        Node<T> temp = head.next;
        temp.previous = null;
        head = temp;
        size--;
        return ret;
    }

    public T popTail() {
        if(tail == null) {
            return null;
        }
        T ret = tail.data;
        Node<T> temp = tail.previous;
        temp.next = null;
        tail = temp;
        size--;
        return ret;
    }

    public T pop(int index) {
        if(index < 0 && index > size - 1) {
            throw new IndexOutOfBoundsException(index);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }
        T ret = temp.data;

        if(temp == head) {
            head.next.previous = null;
            head = head.next;
            size--;
            return ret;
        }

        if(temp == tail) {
            tail.previous.next = null;
            tail = tail.previous;
            size--;
            return ret;
        }

        temp.previous.next = temp.next;
        temp.next.previous = temp.previous;
        temp = null;
        size--;
        return ret;
    }

    public void pushTail(T data) {
        Node<T> newNode = new Node(data);
        if(tail == null) {
            head = newNode;
            tail = head;
            head.next = tail;
            tail.previous = head;
            size++;
            return;
        }
        tail.next = newNode;
        newNode.previous = tail;
        tail = newNode;
        size++;
    }

    public void pushHead(T data) {
        Node<T> newNode = new Node(data);
        if(head == null) {
            head = newNode;
            tail = head;
            head.next = tail;
            tail.previous = head;
            size++;
            current = head;
            return;
        }

        head.previous = newNode;
        newNode.next = head;
        head = newNode;
        size++;
    }

    public void push(T data, int index) {
        if(index < 0 && index > size - 1) {
            throw new IndexOutOfBoundsException(index);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }

        if(temp == head) {
            Node<T> newNode = new Node(data,null,head);
            head.previous = newNode;
            head = newNode;
            size++;
            return;
        }

        Node<T> newNode = new Node(data,temp.previous,temp);
        temp.previous.next = newNode;
        temp.previous = newNode;
        size++;
    }

    public void display() {
        Node<T> temp = head;
        System.out.print("|| ");
        while(temp != null) {
            System.out.print(temp.data + " || ");
            temp = temp.next;
        }
        System.out.println();
    }

}
