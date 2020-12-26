package Kurama.misc_structures.LinkedList;

import java.util.List;

public class CircularDoublyLinkedList<T> extends DoublyLinkedList<T> {

    private boolean shouldStart = true;

    public CircularDoublyLinkedList(Node<T> initNode) {
        this.head = initNode;
        this.tail = head;
        this.head.next = tail;
        this.head.previous = tail;
        this.tail.previous = head;
        this.tail.next = head;

        current = head;
        prev = current.previous;
        next = current.next;
        size++;
        resetLoc();
    }

    public CircularDoublyLinkedList(List<T> list) {
        super(list);
    }

    public CircularDoublyLinkedList() {
        super();
    }

    public T peek(int index) {

        while(index < 0) {
            index = size + index;
        }
        while(index > (size-1)) {
            index = index - (size - 1);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }

        return temp.data;
    }

    public T peekNext() {
        if(current == null) {
            return null;
        }
        if(shouldStart) {
            current = head;
            shouldStart = false;
        }
        else {
            current = current.next;
        }
        return current.data;
    }

    public T peekPrevious() {
        if(current == null) {
            return null;
        }
        current = current.previous;
        return current.data;
    }

    public Node<T> peekNode(int index) {

        while(index < 0) {
            index = size + index;
        }
        while(index > (size-1)) {
            index = index - (size - 1);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }

        return temp;
    }

    public Node<T> peekNextNode() {
        if(current == null) {
            return null;
        }
        if(shouldStart) {
            current = head;
            shouldStart = false;
        }
        else {
            current = current.next;
        }
        return current;
    }

    public Node<T> peekPreviousNode() {
        if(current == null) {
            return null;
        }
        current = current.previous;
        return current;
    }

    public void resetLoc() {
        current = head;
        shouldStart = true;
    }

    public void setLoc(int index) {
        resetLoc();
        for(int i = 0;i<index;i++) {
            peekNext();
        }
    }

    public T popHead() {
        if(head == null) {
            return null;
        }
        T ret = head.data;
        Node<T> temp = head.next;
        temp.previous = tail;
        tail.next = temp;
        head = temp;
        size--;
        return ret;
    }

    public Node<T> popHeadNode() {
        if(head == null) {
            return null;
        }
        Node<T> ret = head;
        Node<T> temp = head.next;
        temp.previous = tail;
        tail.next = temp;
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
        temp.next = head;
        head.previous = temp;
        tail = temp;
        size--;
        return ret;
    }

    public Node<T> popTailNode() {
        if(tail == null) {
            return null;
        }
        Node<T> ret = tail;
        Node<T> temp = tail.previous;
        temp.next = head;
        head.previous = temp;
        tail = temp;
        size--;
        return ret;
    }

    public T pop(int index) {
        while(index < 0) {
            index = size + index;
        }
        while(index > (size-1)) {
            index = index - (size - 1);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }
        T ret = temp.data;

        if(temp == head) {
            head.next.previous = tail;
            tail.next = head.next;
            head = head.next;
            size--;
            return ret;
        }

        if(temp == tail) {
            tail.previous.next = head;
            head.previous = tail.previous;
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

    public Node<T> popNode(int index) {
        while(index < 0) {
            index = size + index;
        }
        while(index > (size-1)) {
            index = index - (size - 1);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }
        Node<T> ret = temp;

        if(temp == head) {
            head.next.previous = tail;
            tail.next = head.next;
            head = head.next;
            size--;
            return ret;
        }

        if(temp == tail) {
            tail.previous.next = head;
            head.previous = tail.previous;
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
            head.previous = tail;
            tail.previous = head;
            tail.next = head;
            size++;
            return;
        }
        tail.next = newNode;
        newNode.previous = tail;
        head.previous = newNode;
        newNode.next = head;
        tail = newNode;
        size++;
    }

    public void pushHead(T data) {
        Node<T> newNode = new Node(data);
        if(head == null) {
            head = newNode;
            tail = head;
            head.next = tail;
            head.previous = tail;
            tail.previous = head;
            tail.next = head;
            size++;
            current = head;
            return;
        }

        head.previous = newNode;
        newNode.next = head;
        newNode.previous = tail;
        tail.next = newNode;
        head = newNode;
        size++;
    }

    public void push(T data, int index) {
        while(index < 0) {
            index = size + index;
        }
        while(index > (size-1)) {
            index = index - (size - 1);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }

        if(temp == head) {
            if(head == null) {
                head = (Node<T>) new Node(data);
                tail = head;
                head.next = tail;
                head.previous = tail;
                tail.next = head;
                tail.previous = head;
            }
            Node<T> newNode = new Node(data,tail,head);
            head.previous = newNode;
            tail.next = newNode;
            head = newNode;
            size++;
            return;
        }

        Node<T> newNode = new Node(data,temp.previous,temp);
        temp.previous.next = newNode;
        temp.previous = newNode;
        size++;
    }

    public Node<T> searchForNode(T element) {
        Node<T> currPos = current;
        boolean shouldStart = this.shouldStart;
        resetLoc();

        Node<T> ret = null;
        for(int i = 0;i < size;i++) {
            Node<T> temp = peekNextNode();
            if(temp.data.equals(element)) {
                ret = temp;
                break;
            }
        }

        this.shouldStart = shouldStart;
        this.current = currPos;

        return ret;
    }

    public Node<T> searchAndRemoveNode(T element) {
        Node<T> currPos = current;
        boolean shouldStart = this.shouldStart;
        resetLoc();

        Node<T> ret = null;
        for(int i = 0;i < size;i++) {
            Node<T> temp = peekNextNode();
            if(temp.data.equals(element)) {
                ret = temp;
                break;
            }
        }

        if(ret != null) {
            if(ret == head) {
                head.next.previous = tail;
                tail.next = head.next;
                head = head.next;
            }

            else if(ret == tail) {
                tail.previous.next = head;
                head.previous = tail.previous;
                tail = tail.previous;
            }

            ret.previous.next = ret.next;
            ret.next.previous = ret.previous;
            size--;
        }

        this.shouldStart = shouldStart;
        this.current = currPos;

        return ret;
    }

    public boolean isPresent(T element) {
        Node<T> currPos = current;
        boolean shouldStart = this.shouldStart;
        resetLoc();

        for(int i = 0;i < size;i++) {
            Node<T> temp = peekNextNode();
            if(temp.data.equals(element)) {
                return true;
            }
        }

        this.shouldStart = shouldStart;
        this.current = currPos;

        return false;
    }

    public void display() {

        Node<T> temp = head;

        System.out.print("|| ");
        if(size > 0) {
            System.out.print(temp.data.toString() + " || ");
            temp = temp.next;
        }
        else {
            System.out.println(" || ");
            return;
        }

        while(temp != head) {
            System.out.print(temp.data.toString() + " || ");
            temp = temp.next;
        }
        System.out.println();
    }

}
