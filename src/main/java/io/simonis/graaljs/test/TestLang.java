package io.simonis.graaljs.test;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

// This is based on the great "Graal Truffle tutorial" by Adam Ruka:
// https://www.endoflineblog.com/graal-truffle-tutorial-part-1-setup-nodes-calltarget
public class TestLang {

  @TypeSystemReference(TestTypeSystem.class)
  public static abstract class TestNode extends Node {
    public abstract int executeInt(VirtualFrame frame) throws UnexpectedResultException;
    public abstract double executeDouble(VirtualFrame frame);
    public abstract Object executeGeneric(VirtualFrame frame);
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
    @Override
    public double executeDouble(VirtualFrame frame) {
        return this.value;
    }
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return this.value;
    }
  }

  public static final class DoubleLiteralNode extends TestNode {
    private final double value;

    public DoubleLiteralNode(double value) {
      this.value = value;
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
      throw new UnexpectedResultException(this.value);
    }
    @Override
    public double executeDouble(VirtualFrame frame) {
        return this.value;
    }
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return this.value;
    }
  }

  public static final class ReadArgExprNode extends TestNode {
    private final int index;

    public ReadArgExprNode(int index) {
      this.index = index;
    }
    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
      Object[] arguments = frame.getArguments();
      Object ret = arguments[index];
      if (ret instanceof Integer) {
        return (int)ret;
      }
      throw new UnexpectedResultException(ret);
    }
    @Override
    public double executeDouble(VirtualFrame frame) {
      Object[] arguments = frame.getArguments();
      Object ret = arguments[index];
      if (ret instanceof Integer) {
        return (int)ret;
      }
      if (ret instanceof Double) {
        return (double)ret;
      }
      return (double)ret; // This will throw a ClassCastException
    }
    @Override
    public Object executeGeneric(VirtualFrame frame) {
      Object[] arguments = frame.getArguments();
      return arguments[index];
    }
  }

  public static final class AdditionNode extends TestNode {
    @Child
    private TestNode leftNode, rightNode;

    private enum SpecializationState { UNINITIALIZED, INT, DOUBLE }

    @CompilerDirectives.CompilationFinal
    private SpecializationState specializationState;

    public AdditionNode(TestNode leftNode, TestNode rightNode) {
      this.leftNode = leftNode;
      this.rightNode = rightNode;
      this.specializationState = SpecializationState.UNINITIALIZED;
    }

    @Override
    public int executeInt(VirtualFrame frame)  throws UnexpectedResultException {
      int leftValue;
      try {
        leftValue = this.leftNode.executeInt(frame);
      } catch (UnexpectedResultException e) {
        this.activateDoubleSpecialization();
        double leftDouble = (double) e.getResult();
        throw new UnexpectedResultException(leftDouble + this.rightNode.executeDouble(frame));
      }

      int rightValue;
      try {
        rightValue = this.rightNode.executeInt(frame);
      } catch (UnexpectedResultException e) {
        this.activateDoubleSpecialization();
        double rightDouble = (double) e.getResult();
        throw new UnexpectedResultException(leftValue + rightDouble);
      }

      try {
        return Math.addExact(leftValue, rightValue);
      } catch (ArithmeticException e) {
        this.activateDoubleSpecialization();
        throw new UnexpectedResultException((double) leftValue + (double) rightValue);
      }
    }
    @Override
    public double executeDouble(VirtualFrame frame) {
        double leftValue = this.leftNode.executeDouble(frame);
        double rightValue = this.rightNode.executeDouble(frame);
        return leftValue + rightValue;
    }
    @Override
    public Object executeGeneric(VirtualFrame frame) {
      if (this.specializationState == SpecializationState.INT) {
        try {
          return this.executeInt(frame);
        } catch (UnexpectedResultException e) {
          this.activateDoubleSpecialization();
          return e.getResult();
        }
      }
      if (this.specializationState == SpecializationState.DOUBLE) {
        return this.executeDouble(frame);
      }
      // uninitialized case
      Object leftValue = this.leftNode.executeGeneric(frame);
      Object rightValue = this.rightNode.executeGeneric(frame);
      CompilerDirectives.transferToInterpreterAndInvalidate();
      return this.executeAndSpecialize(leftValue, rightValue);
    }

    private Object executeAndSpecialize(Object leftValue, Object rightValue) {
      if (leftValue instanceof Integer && rightValue instanceof Integer) {
        try {
          int result = Math.addExact((int) leftValue, (int) rightValue);
          this.activateIntSpecialization();
          return result;
        } catch (ArithmeticException e) {
          // fall through to the double case below
        }
      }
      this.activateDoubleSpecialization();
      // one or both of the values might be Integers,
      // because of the && above, and the possibility of overflow
      return convertToDouble(leftValue) + convertToDouble(rightValue);
    }

    private void activateIntSpecialization() {
      this.specializationState = SpecializationState.INT;
    }
    private void activateDoubleSpecialization() {
      this.specializationState = SpecializationState.DOUBLE;
    }

    private static double convertToDouble(Object value) {
      if (value instanceof Integer) {
        return ((Integer) value).doubleValue();
      }
      return (double) value; // This will throw a ClassCastException if 'value' is not a 'Double'
    }
  }
  /*
  public final class FunctionCallExprNode extends TestNode {
    @Child
    private TestNode targetExpression;
    @Children
    private final TestNode[] callArguments;
    @Child
    private final DirectCallNode function;

    public FunctionCallExprNode(TestNode targetExpression, List<TestNode> callArguments) {
        this.targetExpression = targetExpression;
        this.callArguments = callArguments.toArray(new TestNode[]{});
        this.dispatchNode = FunctionDispatchNodeGen.create();
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object[] argumentValues = new Object[this.callArguments.length];
        for (int i = 0; i < this.callArguments.length; i++) {
            argumentValues[i] = this.callArguments[i].executeGeneric(frame);
        }

        return this.dispatchNode.executeDispatch(targetExression, argumentValues);
    }
  }
  */
  @TypeSystem
  public static abstract class TestTypeSystem {
    @ImplicitCast
    public static double castIntToDouble(int value) {
      return value;
    }
  }

  @NodeChild("leftNode")
  @NodeChild("rightNode")
  public static abstract class DSLAdditionNode extends TestNode {
    @Specialization(rewriteOn = ArithmeticException.class)
    protected int addInts(int leftValue, int rightValue) {
      return Math.addExact(leftValue, rightValue);
    }

    @Specialization(replaces = "addInts")
    protected double addDoubles(double leftValue, double rightValue) {
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
      return this.exprNode.executeGeneric(frame);
    }
  }

  public static void main(String[] args) throws Exception {
    TestNode constAdd = new AdditionNode(
      new IntLiteralNode(12),
      new IntLiteralNode(30));
    RootNode rootNode = new TestRootNode(constAdd);
    CallTarget callTarget = rootNode.getCallTarget();
    Object result = callTarget.call();
    System.out.println(result);
    DirectCallNode constAddFunction = DirectCallNode.create(callTarget);

    TestNode varAdd = new AdditionNode(
      new ReadArgExprNode(0),
      new ReadArgExprNode(1));
    rootNode = new TestRootNode(varAdd);
    callTarget = rootNode.getCallTarget();
    result = callTarget.call(12, 30);
    System.out.println(result);
    DirectCallNode addFunction = DirectCallNode.create(callTarget);
    result = addFunction.call(12, 30);
    System.out.println(result);
    for (int i = 1 ; i <= 10_000; i++) {
      //result = callTarget.call(i, i);
      result = addFunction.call(12, 30);
      assert (int)result == i + i;
    }

    TestNode dslExprNode = TestLangFactory.DSLAdditionNodeGen.create(
      new IntLiteralNode(12),
      new IntLiteralNode(30));
    rootNode = new TestRootNode(dslExprNode);
    callTarget = rootNode.getCallTarget();
    result = callTarget.call();
    System.out.println(result);
  }
}
