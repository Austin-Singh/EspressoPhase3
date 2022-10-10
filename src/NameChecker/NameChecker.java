package NameChecker;

import AST.*;
import Utilities.Error;
import Utilities.SymbolTable;
import Utilities.Visitor;
import Utilities.Rewrite;

import java.util.*;
import Parser.*;;

public class NameChecker extends Visitor {

	/* getMethod traverses the class hierarchy to look for a method of
       name 'methodName'. We return #t if we find any method with the
       correct name. Since we don't have types yet we cannot look at
       the signature of the method, so all we do for now is look if
       any method is defined. The search is as follows:

       1) look in the current class
       2) look in its super class
       3) look in all the interfaces

       Remember that the an entry in the methodTable is a symbol table
       it self. It holds all entries of the same name, but with
       different signatures. (See Documentation)
	 */    
	public static SymbolTable getMethod(String methodName, ClassDecl cd) {

		// OUR CODE HERE
		return null;
	}

	/* Same as getMethod just for fields instead 
	 */
	public static AST getField(String fieldName, ClassDecl cd) {

		// OUR CODE HERE
		return null;
	}

	/* Traverses all the classes and interfaces and builds a sequence
	   of the methods and constructors of the class hierarchy.
	 */
    public void getClassHierarchyMethods(ClassDecl cd, Sequence lst, Hashtable<String, Object> seenClasses) {
		// OUR CODE HERE
    }

	/* For each method (not constructors) in this list, check that if
       it exists more than once with the same parameter signature that
       they all return something of the same type. 
	*/
    public void checkReturnTypesOfIdenticalMethods(Sequence lst) {
		// OUR CODE HERE
    }
    
    /* Divides all the methods into two sequences: one for all the
       abstract ones, and one for all the concrete ones and check that
       all the methods that were delcared abstract were indeed
       implemented somewhere in the class hierarchy.  */
    

    // sup is the class in which the abstract method lives,
    // sub is the class in which the concrete method lives.
    public static boolean isSuper(ClassDecl sup, ClassDecl sub) {
		if (sup.name().equals(sub.name()))
			return true;
			
		if (sub.superClass() != null && isSuper(sup, sub.superClass().myDecl))
			return true;

		for (int i=0; i<sub.interfaces().nchildren; i++) 
			if (isSuper(sup, ((ClassType)sub.interfaces().children[i]).myDecl))
			return true;

		return false;
    }


	public void checkImplementationOfAbstractClasses(ClassDecl cd, Sequence methods) {
		// OUR CODE HERE
	}
    
	public  void checkUniqueFields(Sequence fields, ClassDecl cd) {
		// OUR CODE HERE
	}

	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

	/**
	 * Points to the current scope.
	 */
	private SymbolTable currentScope;
	/**
	 * The class table<br>
	 * This is set in the constructor.
	 */
	private SymbolTable classTable;
	/**
	 * The current class in which we are working.<br>
	 * This is set in visitClassDecl().
	 */
	private ClassDecl   currentClass;
	
	public NameChecker(SymbolTable classTable, boolean debug) { 
		this.classTable = classTable; 
		this.debug = debug;
	}

	/** (1) BLOCK */
	public Object visitBlock(Block bl) {
		println("Block:\t\t Creating new scope for Block.");
		currentScope = currentScope.newScope();
		super.visitBlock(bl);
		currentScope = currentScope.closeScope(); 
		return null;
	}

	/** (2) FOR STAT */
	public Object visitForStat(ForStat bl) {
		// OUR CODE HERE (COMPLETE)
		println("ForStat:\t\t Creating new scope for For Statement.");
		currentScope = currentScope.newScope();
		super.visitForStat(bl);
		currentScope = currentScope.closeScope(); 
		return null;
	}

	/** (3) CONSTRUCTOR DECLARATION */
	public Object visitConstructorDecl(ConstructorDecl bl) {
		// OUR CODE HERE (COMPLETE)
		println("ConstructorDecl: Creating new scope for constructor <init> with signature '" + bl.paramSignature() + "' (Parameters and Locals).");
		currentScope = currentScope.newScope();
		super.visitSequence(bl.params());
		currentScope = currentScope.newScope();
		if (bl.cinvocation() != null) {
			super.visitCInvocation(bl.cinvocation());
		}
		super.visitSequence(bl.body());
		currentScope = currentScope.closeScope();
		currentScope = currentScope.closeScope();
		return null;
	}

	/** (4) METHOD DECLARATION */
	public Object visitMethodDecl(MethodDecl bl) {
		// OUR CODE HERE (COMPLETE)
		println("MethodDecl:\t Creating new scope for Method '"+ bl.getname() + "' with signature '" + bl.paramSignature() + "' (Parameters and Locals).");
		currentScope = currentScope.newScope();
		super.visitMethodDecl(bl);
		currentScope = currentScope.closeScope(); 
		return null;
	}

	/** (5) SWITCH STAT */
	public Object visitSwitchStat(SwitchStat bl) {
		// OUR CODE HERE (COMPLETE)
		println("SwitchStat:\t\t Creating new scope for SwitchStat.");
		currentScope = currentScope.newScope();
		super.visitSwitchStat(bl);
		currentScope = currentScope.closeScope(); 
		return null;
	}

	/** (6) CLASS DECLARATION */
	public Object visitClassDecl(ClassDecl cd) {
		println("ClassDecl:\t Visiting class '"+cd.name()+"'");

		// If we use the field table here as the top scope, then we do not
		// need to look in the field table when we resolve NameExpr. Note,
		// at this time we have not yet rewritten NameExprs which are really
		// FieldRefs with a null target as we have not resolved anything yet.
		currentScope = cd.fieldTable;
		currentClass = cd;

		Hashtable<String, Object> seenClasses = new Hashtable<String, Object>();

		// Check that the superclass is a class.
		if (cd.superClass() != null) 
			if (cd.superClass().myDecl.isInterface())
				Error.error(cd,"Class '" + cd.name() + "' cannot inherit from interface '" +
						cd.superClass().myDecl.name() + "'.");



		if (cd.superClass() != null) {
			if (cd.name().equals(cd.superClass().typeName()))
				Error.error(cd, "Class '" + cd.name() + "' cannot extend itself.");
			// If a superclass has a private default constructor, the 
			// class cannot be extended.
			ClassDecl superClass = (ClassDecl)classTable.get(cd.superClass().typeName());
			SymbolTable st = (SymbolTable)superClass.methodTable.get("<init>");
			ConstructorDecl ccd = (ConstructorDecl)st.get("");
			if (ccd != null && ccd.getModifiers().isPrivate())
			    Error.error(cd, "Class '" + superClass.className().getname() + "' cannot be extended because it has a private default constructor.");
		}
		
		// Visit the children
		super.visitClassDecl(cd);
			
		currentScope = null;

		// Check that the interfaces implemented are interfaces.
		for (int i=0; i<cd.interfaces().nchildren; i++) {
			ClassType ct = (ClassType)cd.interfaces().children[i];
			if (ct.myDecl.isClass())
				Error.error(cd,"Class '" + cd.name() + "' cannot implement class '" + ct.name() + "'.");
		}

		Sequence methods = new Sequence();
		
		getClassHierarchyMethods(cd, methods, seenClasses);

		checkReturnTypesOfIdenticalMethods(methods);

		// If the class is not abstract and not an interface it must implement all
		// the abstract functions of its superclass(es) and its interfaces.
		if (!cd.isInterface() && !cd.modifiers.isAbstract()) {
			checkImplementationOfAbstractClasses(cd, methods);
			// checkImplementationOfAbstractClasses(cd, new Sequence());
		}
		// All field names can only be used once in a class hierarchy
		checkUniqueFields(new Sequence(), cd);

		cd.allMethods = methods; // now contains only MethodDecls

		// Fill cd.constructors.
		SymbolTable st = (SymbolTable)cd.methodTable.get("<init>");
		ConstructorDecl cod;
		if (st != null) {
			for (Enumeration<Object> e = st.entries.elements() ; 
					e.hasMoreElements(); ) {
				cod = (ConstructorDecl)e.nextElement();
				cd.constructors.append(cod);
			}
		}

		// needed for rewriting the tree to replace field references
		// represented by NameExpr.
		println("ClassDecl:\t Performing tree Rewrite on " + cd.name());
		new Rewrite().go(cd, cd);

		return null;
	}

	/** (7) LOCAL DELCARATION */
	public Object visitLocalDecl(LocalDecl bl) {
		// OUR CODE HERE ("these are one liners") - (COMPLETE)
		println("LocalDecl:\t Declaring local symbol '" + bl.var() + "'.");
		currentScope.put(bl.name(), this);
		return null;
	}

	/** (8) PARAM DELCARATION */
	public Object visitParamDecl(ParamDecl bl) {
		// OUR CODE HERE ("these are one liners") - (COMPLETE)
		println("ParamDecl:\t Declaring parameter '" + bl.paramName() + "'.");
		currentScope.put(bl.name(), this);
		return null;
	}

	/** (9) NAME EXPRESSION */
	public Object visitNameExpr(NameExpr bl) {
		// OUR CODE HERE
		/**
			Could be the name of: LocalDecal, ParamDecl, FieldDecl, ClassDecal
			NAME EXPRESSION: has a field called myDecl, set it euqal to the result of the lookup we get from below:

			1) Look in the sym.tab chain (represented by CurrentScope)
				- if we get null back, then we know it is not a LocalDecl.

			2) Look in CurrentClass's Field Table
				- if we we get null back, then look in global class table.
					- if we get null back, throw an ERROR

			STRING OUTPUT IN REFERENCE COMPILER OUTPUT
			1) "NameExpr:        Looking up symbol 'a'."
			2) "Found Local Variable"
		*/

		return null;
	}

	/** (10) INVOCATION */
	public Object visitInvocation() {
		// OUR CODE HERE
		// INOCATION: expr.function(...)
		// we will end up calling getMethod()
		// - expr can be anything only if it (target) is null or an instanceof this -> look in the table of currentClass

		return null;
	}

	/** (11) FIELD REFERENCE */
	public Object visitFieldRef() {
		// OUR CODE HERE
		// we will end up calling getField()
		// FIELD REF: expr.field
		// - you will never be in the situation where the target of the fieldref is null
		// - expr can be anything only if it (target) is an instanceof this -> look in the table of currentClass (?)

		return null;
	}

	/** (13) CLASS TYPE */
	public Object visitClassType(ClassType bl) {
		// OUR CODE HERE ("these are one liners") - (COMPLETE)
		println("ClassType:\t Looking up class/interface '" + bl.typeName() + "' in class table.");
		classTable.get(bl.typeName());
		return null;
	}

	/** (12) THIS */
	public Object visitThis(This th) {
		ClassType ct = new ClassType(new Name(new Token(16,currentClass.name(),0,0,0)));
		ct.myDecl = currentClass;
		th.type = ct;
		return null;
	}

}

