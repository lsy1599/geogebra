package org.geogebra.common.kernel.stepbystep.steptree;

import org.geogebra.common.kernel.stepbystep.solution.SolutionBuilder;
import org.geogebra.common.kernel.stepbystep.solution.SolutionStepType;
import org.geogebra.common.kernel.stepbystep.steps.RegroupTracker;
import org.geogebra.common.kernel.stepbystep.steps.SimplificationStepGenerator;
import org.geogebra.common.kernel.stepbystep.steps.StepStrategies;
import org.geogebra.common.plugin.Operation;

public abstract class StepTransformable extends StepNode {

	protected int color;

	public abstract boolean contains(Operation op);

	public abstract StepTransformable iterateThrough(SimplificationStepGenerator step,
			SolutionBuilder sb, RegroupTracker tracker);

	public abstract StepSolvable toSolvable();

	public abstract boolean isOperation(Operation op);

	StepTransformable regroup() {
		return regroup(null);
	}

	/**
	 * This is the default regroup. Assumes every nonSpecialConstant is an integer.
	 *
	 * @param sb SolutionBuilder for the regroup steps
	 * @return the expression, regrouped
	 */
	public StepTransformable regroup(SolutionBuilder sb) {
		return StepStrategies.defaultRegroup(this, sb);
	}

	public StepTransformable weakRegroup() {
		return StepStrategies.weakRegroup(this, null);
	}

	/**
	 * Numeric regroup. Evaluates expressions like 1/3 and sqrt(2)..
	 *
	 * @param sb SolutionBuilder for the regroup steps
	 * @return the expression, regrouped
	 */
	public StepTransformable numericRegroup(SolutionBuilder sb) {
		return StepStrategies.decimalRegroup(this, sb);
	}

	public StepTransformable convertToFractions(SolutionBuilder sb) {
		return StepStrategies.convertToFraction(this, sb);
	}

	public StepTransformable adaptiveRegroup() {
		return adaptiveRegroup(null);
	}

	public StepTransformable adaptiveRegroup(SolutionBuilder sb) {
		if (0 < maxDecimal() && maxDecimal() < 5 && containsFractions()) {
			StepTransformable temp = convertToFractions(sb);
			return temp.regroup(sb);
		}

		if (maxDecimal() > 0) {
			return numericRegroup(sb);
		}

		return regroup(sb);
	}

	public StepTransformable regroupOutput(SolutionBuilder sb) {
		sb.add(SolutionStepType.SIMPLIFY, this);
		return adaptiveRegroup(sb);
	}

	/**
	 * @return the expression, regrouped and expanded
	 */
	public StepTransformable expand() {
		return expand(new SolutionBuilder());
	}

	/**
	 * @param sb SolutionBuilder for the expansion steps
	 * @return the expression, regrouped and expanded
	 */
	public StepTransformable expand(SolutionBuilder sb) {
		return StepStrategies.defaultExpand(this, sb);
	}

	public StepTransformable expandOutput(SolutionBuilder sb) {
		sb.add(SolutionStepType.EXPAND, this);
		return expand(sb);
	}

	/**
	 * @return the expression, factored
	 */
	public StepTransformable factor() {
		return factor(null);
	}

	/**
	 * @param sb SolutionBuilder for the factoring steps
	 * @return the expression, factored
	 */
	public StepTransformable factor(SolutionBuilder sb) {
		return StepStrategies.defaultFactor(this, sb);
	}

	public StepTransformable weakFactor(SolutionBuilder sb) {
		return StepStrategies.weakFactor(this, sb);
	}

	public StepTransformable factorOutput(SolutionBuilder sb) {
		sb.add(SolutionStepType.FACTOR, this);
		return factor(sb);
	}

	public StepTransformable differentiate() {
		return differentiate(null);
	}

	public StepTransformable differentiate(SolutionBuilder sb) {
		return StepStrategies.defaultDifferentiate(this, sb);
	}

	public StepTransformable differentiateOutput(SolutionBuilder sb) {
		sb.add(SolutionStepType.DIFFERENTIATE, this);
		return differentiate(sb);
	}

	public abstract int maxDecimal();

	public abstract boolean containsFractions();

	protected String getColorHex() {
		return getColorHex(color);
	}

	/**
	 * Recursively sets a color for the tree (i.e. for the root and all of the nodes
	 * under it)
	 *
	 * @param color the color to set
	 */
	public abstract void setColor(int color);

	/**
	 * Sets 0 as the color of the tree
	 */
	public void cleanColors() {
		setColor(0);
	}
}
