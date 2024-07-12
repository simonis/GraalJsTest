package io.simonis.graaljs.test;

import com.oracle.truffle.api.CallTarget;;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

// This is based on the great "Graal Truffle tutorial" by Adam Ruka:
// https://www.endoflineblog.com/graal-truffle-tutorial-part-1-setup-nodes-calltarget
public class TestLang {

  public static abstract class TestNode extends Node {
    public abstract int executeInt(VirtualFrame frame);
  }

  public static final class IntLiteralNode extends TestNode {
    private final int value;

    public IntLiteralNode(int value) {
      this.value = value;
    }

    @Override
    public int executeInt(VirtualFrame frame) {
      return this.value;
    }
  }

  public static final class AdditionNode extends TestNode {
    @Child
    private TestNode leftNode, rightNode;

    public AdditionNode(TestNode leftNode, TestNode rightNode) {
      this.leftNode = leftNode;
      this.rightNode = rightNode;
    }

    @Override
    public int executeInt(VirtualFrame frame) {
      int leftValue = this.leftNode.executeInt(frame);
      int rightValue = this.rightNode.executeInt(frame);
      return leftValue + rightValue;
    }
  }

  public static final class TestRootNode extends RootNode {
    @Child
    private TestNode exprNode;

    public TestRootNode(TestNode exprNode) {
      super(null);
      this.exprNode = exprNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
      return this.exprNode.executeInt(frame);
    }
  }

  public static void main(String[] args) throws Exception {
    TestNode exprNode = new AdditionNode(
      new IntLiteralNode(12),
      new IntLiteralNode(30));
    RootNode rootNode = new TestRootNode(exprNode);
    CallTarget callTarget = rootNode.getCallTarget();
    Object result = callTarget.call();
    System.out.println(result);
    for (int i = 1 ; i <= 10_000; i++) {
      callTarget.call();
    }
  }
}
