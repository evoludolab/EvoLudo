package org.evoludo.simulator.models;

import org.evoludo.simulator.EvoLudo;
import org.evoludo.simulator.models.Model.Discrete;

/**
 * Common abstract base class for all differential equations models.
 * <p>
 * <strong>Note:</strong> Currently differential equation models are restricted
 * to discrete strategy sets.
 */
public abstract class DE extends Model implements Discrete {

	public DE(EvoLudo engine) {
		super(engine);
	}

	/**
	 * Return whether this DE model tracks frequencies or densities. Returns
	 * <code>false</code> (i.e. frequency based model) by default.
	 *
	 * @return <code>true</code> if state refers to densities.
	 */
	public boolean isDensity() {
		return false;
	}

	/**
	 * Sets the discretization of time increments in continuous time models.
	 * <p>
	 * <strong>Note:</strong> Some models may need to adjust, i.e. reduce,
	 * <code>dt</code> (see {@link PDERD#checkDt()}) or choose a variable step size
	 * in which case <code>dt</code> is ignored (see {@link ODERK#getAutoDt()}).
	 *
	 * @param dt the time increments in continuous time models.
	 */
	public abstract void setDt(double dt);

	/**
	 * Gets the discretization of time increments in continuous time models.
	 * <p>
	 * <strong>Note:</strong> This may be different from <code>dt</code> set through
	 * {@link #setDt(double)} if the model required adjustments.
	 *
	 * @return the time increment in continuous time models.
	 */
	public abstract double getDt();

	/**
	 * Sets the desired accuracy for determining convergence. If
	 * <code>y(t+dt)-y(t)&lt;a dt</code> holds, where <code>y(t+dt)</code> denotes
	 * the new state and <code>y(t)</code> the previous state, the numerical
	 * integration is reported as having converged and stops.
	 *
	 * @param accuracy the numerical accuracy
	 */
	public abstract void setAccuracy(double accuracy);

	/**
	 * Gets the numerical accuracy for determining convergence.
	 *
	 * @return the numerical accuracy
	 */
	public abstract double getAccuracy();
}
