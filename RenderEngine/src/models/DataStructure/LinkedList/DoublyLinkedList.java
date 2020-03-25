package models.DataStructure.LinkedList;

import java.util.Iterator;
import java.util.List;

public class DoublyLinkedList<T> {

    protected int size = 0;
    protected Node<T> prev = null;
    protected Node<T> current = null;
    protected Node<T> next = null;
    protected boolean isOverRightEnd = false;
    protected  boolean isAtRightEnd = false;
    protected boolean isOverLeftEnd = false;
    protected boolean isAtLeftEnd = false;

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

        resetLoc();
    }

    public DoublyLinkedList(List<T> list) {
        for(T element:list) {
            this.pushTail(element);
        }
        resetLoc();
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

    public T peek(int index) {

        if(index < 0) {
            index = size + index;
        }

        if(index < 0 || index > size - 1) {
            throw new IndexOutOfBoundsException(index);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }

        return temp.data;
    }

    public T peekHead() {
        return head.data;
    }

    public T peekTail() {
        return tail.data;
    }

    public T peekNext() {
        if(next == null) {
            if (isAtRightEnd) {
                isOverRightEnd = true;
                return null;
            }
        }
        else if(isOverLeftEnd) {
            isOverLeftEnd = false;
            return current.data;
        }


        isAtLeftEnd = false;
        prev = current;
        current = current.next;
        next = current.next;

        if(next == null) {
            isAtRightEnd = true;
        }

        return current.data;
    }

    public T peekPrevious() {

        if(prev == null) {
            if(isAtLeftEnd) {
                isOverLeftEnd = true;
                return null;
            }
        }
        else if(isOverRightEnd) {
            isOverRightEnd = false;
            return current.data;
        }

        isAtRightEnd = false;
        next = current;
        current = current.previous;
        prev = current.previous;

        if(prev == null) {
            isAtLeftEnd = true;
        }

        return current.data;
    }

    public Node<T> peekNode() {
        if(current == null) {
            return null;
        }
        return current;
    }

    public Node<T> peekNode(int index) {

        if(index < 0) {
            index = size + index;
        }

        if(index < 0 || index > size - 1) {
            throw new IndexOutOfBoundsException(index);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }

        return temp;
    }

    public Node<T> peekHeadNode() {
        return head;
    }

    public Node<T> peekTailNode() {
        return tail;
    }

    public Node<T> peekNextNode() {
        if(next == null) {
            if (isAtRightEnd) {
                isOverRightEnd = true;
                return null;
            }
        }
        else if(isOverLeftEnd) {
            isOverLeftEnd = false;
            return current;
        }


        isAtLeftEnd = false;
        prev = current;
        current = current.next;
        next = current.next;

        if(next == null) {
            isAtRightEnd = true;
        }

        return current;
    }

    public Node<T> peekPreviousNode() {

        if(prev == null) {
            if(isAtLeftEnd) {
                isOverLeftEnd = true;
                return null;
            }
        }
        else if(isOverRightEnd) {
            isOverRightEnd = false;
            return current;
        }

        isAtRightEnd = false;
        next = current;
        current = current.previous;
        prev = current.previous;

        if(prev == null) {
            isAtLeftEnd = true;
        }

        return current;
    }

    public void setLoc(Node<T> loc) {
        if(loc == head) {
            resetLoc();
        }
        else {
            this.current = loc;
            prev = current.previous;
            next = current.next;
        }
    }

    public void resetLoc() {
        current = head;
        next = current.next;
        prev = current.previous;
        isAtRightEnd = false;
        isAtLeftEnd = true;
        isOverLeftEnd = true;
        isOverRightEnd = false;
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

    public Node<T> popHeadNode() {
        if(head == null) {
            return null;
        }
        Node<T> ret = head;
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

    public Node<T> popTailNode() {
        if(tail == null) {
            return null;
        }
        Node<T> ret = tail;
        Node<T> temp = tail.previous;
        temp.next = null;
        tail = temp;
        size--;
        return ret;
    }

    public T pop(int index) {
        if(index < 0) {
            index = size + index;
        }

        if(index < 0 || index > size - 1) {
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

    public Node<T> popNode(int index) {
        if(index < 0) {
            index = size + index;
        }

        if(index < 0 || index > size - 1) {
            throw new IndexOutOfBoundsException(index);
        }

        Node<T> temp = head;
        for(int i = 0;i < index;i++) {
            temp = temp.next;
        }
        Node<T> ret = temp;

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
        if(index < 0) {
            index = size + index;
        }
        if(index < 0 || index > size - 1) {
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

    public Node<T> searchForNode(T element) {
        Node<T> currPos = current;
        boolean isOverRightEnd = this.isOverRightEnd;
        boolean isAtRightEnd = this.isAtRightEnd;
        boolean isOverLeftEnd = this.isOverLeftEnd;
        boolean isAtLeftEnd = this.isAtLeftEnd;
        resetLoc();

        Node<T> ret = null;
        for(int i = 0;i < size;i++) {
            Node<T> temp = peekNextNode();
            if(temp.data.equals(element)) {
                ret = temp;
                break;
            }
        }

        this.isOverRightEnd = isOverRightEnd;
        this.isOverLeftEnd = isOverLeftEnd;
        this.isAtRightEnd = isAtRightEnd;
        this.isAtLeftEnd = isAtLeftEnd;
        this.current = currPos;

        return ret;
    }

    public Node<T> searchAndRemoveNode(T element) {
        Node<T> currPos = current;
        boolean isOverRightEnd = this.isOverRightEnd;
        boolean isAtRightEnd = this.isAtRightEnd;
        boolean isOverLeftEnd = this.isOverLeftEnd;
        boolean isAtLeftEnd = this.isAtLeftEnd;
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
                head.next.previous = null;
                head = head.next;
                size--;
            }

            if(ret == tail) {
                tail.previous.next = null;
                tail = tail.previous;
                size--;
            }

            ret.previous.next = ret.next;
            ret.next.previous = ret.previous;
        }

        this.isOverRightEnd = isOverRightEnd;
        this.isOverLeftEnd = isOverLeftEnd;
        this.isAtRightEnd = isAtRightEnd;
        this.isAtLeftEnd = isAtLeftEnd;
        this.current = currPos;

        return ret;
    }

    public boolean isPresent(T element) {
        Node<T> currPos = current;
        boolean isOverRightEnd = this.isOverRightEnd;
        boolean isAtRightEnd = this.isAtRightEnd;
        boolean isOverLeftEnd = this.isOverLeftEnd;
        boolean isAtLeftEnd = this.isAtLeftEnd;
        resetLoc();

        for(int i = 0;i < size;i++) {
            Node<T> temp = peekNextNode();
            if(temp.data.equals(element)) {
                return true;
            }
        }

        this.isOverRightEnd = isOverRightEnd;
        this.isOverLeftEnd = isOverLeftEnd;
        this.isAtRightEnd = isAtRightEnd;
        this.isAtLeftEnd = isAtLeftEnd;
        this.current = currPos;

        return false;
    }

    public void display() {
        Node<T> temp = head;
        System.out.print("|| ");
        while(temp != null) {
            System.out.print((temp.data).toString() + " || ");
            temp = temp.next;
        }
        System.out.println();
    }

    public int getSize() {
        return size;
    }

    public boolean hasNext() {
        return !(isAtRightEnd || isOverRightEnd);
    }
}
