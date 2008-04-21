/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jltl2dstar;

/** @file
 * Implementation of LTL2DSTAR_Scheduler
 */
import java.util.Vector;
import java.io.PrintStream;

import jltl2ba.APSet;
import jltl2ba.SimpleLTL;
import prism.PrismException;

/**
 * Allows translation of LTL to DRA/DSA
 * in multiple ways and combines the basic building blocks to choose the most
 * efficient.
 */
public class Scheduler {

	/** The LTL2DRA wrapper for Safra's algorithm and the external LTL->NBA translator */
	private LTL2DRA _ltl2dra;

	/** Use limiting? */
	private boolean _opt_limits;

	/** The limiting factor */
	private double _alpha;

	/** Print stats on the NBA? */
	private boolean _stat_NBA;
	
	/** Base class for the building blocks for the scheduler */
	public abstract static class Tree {
		protected SimpleLTL _ltl;    
		protected Options_LTL2DRA _options;
		protected int priority;
		protected DRA _automaton;
		protected String _comment;
		protected Scheduler _sched;
		protected APSet _apset;

		protected Vector<Tree> children;

		/**
		 * Constructor 
		 * @param ltl The LTL formula
		 * @param options the LTL2DSTAR options
		 * @param sched a reference back to the scheduler 
		 */
		public Tree(SimpleLTL ltl, APSet apset, Options_LTL2DRA options, Scheduler sched) {
			_ltl = ltl;
			_apset = apset;
			_options = options;
			_sched = sched;
			children = new Vector<Tree>();
		}

		/** Print the tree on output stream (default level: 0) */
		public void printTree(PrintStream out, int level) {
			for (int i = 0; i < level; i++) {
				out.print(" ");
			}
			out.println(this.getClass().getName() + " = " + this + "(" + _ltl + ")");
			for (Tree child : children) {
				child.printTree(out, level + 1);
			}
		}

		/** Abstract virtual function for tree generation */
		public abstract void generateTree();

		/** Estimate the size of the automaton */
		public int guestimate() {
			return 0;
		}

		/** Hook that is called after calculate() finishes */
		public void hook_after_calculate() {};

		/** Calculate the automaton for this building block, by default
		 * calculate the automata for the children and then choose the smallest.
		 * (default level: 0, default limit: 0 */
		public void calculate(int level, int limit) throws PrismException {
			if (_options.verbose_scheduler) {
				System.err.println("Calculate (" + level + "): " + this.getClass().getName());
			}

			calculateChildren(level, limit);

			boolean first = true;
			for (Tree child : children) {
				if (child._automaton == null) {
					continue;
				}

				if (first) {
					_automaton = child._automaton;
					_comment = child._comment;
				} else {
					if (_automaton.size() > child._automaton.size()) {
						_automaton = child._automaton;
						_comment = child._comment;
					}
				}

				first = false;
			}

			hook_after_calculate();
		}

		/** Add a new child */
		public void addChild(Tree child) {
			if (child == null) {return;}

			children.add(child);
		}

		/** Calculate the automata for the children
		 *  (default level: 0, default limit: 0 */
		private void calculateChildren(int level, int limit) throws PrismException {
			if (_sched.flagOptLimits()) {
				DRA _min_automaton;
				int _min_size = 0;

				for (Tree child : children) {
					int child_limit;
					if (_min_size != 0) {
						if (limit > 0) {
							child_limit = _sched.calcLimit(_min_size) < limit ? _sched.calcLimit(_min_size) : limit;
						} else {
							child_limit = _sched.calcLimit(_min_size);
						}
					} else {
						child_limit = limit;
					}
					if (_options.verbose_scheduler) {
						System.err.println(" Limit (with alpha) = " + child_limit);
					}
					try {
						child.calculate(level + 1, child_limit);

						if (child._automaton != null) {
							if (_min_size == 0 || child._automaton.size() < _min_size) {
								_min_automaton = child._automaton;
								_min_size = _min_automaton.size();
							} else {
								// delete automaton as it is bigger
								// than necessary
								child._automaton = null;
							}
						}
					} catch (PrismException e) {
						child._automaton = null;
						throw e;
					}
				}
			} else {
				for (Tree child : children) {
					child.calculate(level + 1, limit);
				}
			}
		}
	}

	/** The root building block for the calculation of DRA/DSA */
	public static class Tree_Start extends Tree {
		/**
		 * Constructor 
		 * @param ltl The LTL formula
		 * @param options the LTL2DSTAR options
		 * @param sched a reference back to the scheduler 
		 */
		public Tree_Start(SimpleLTL ltl, APSet apset, Options_LTL2DRA options, Scheduler sched) {
			super(ltl, apset, options, sched);
			generateTree();
		}

		/** Generate the tree */
		public void generateTree() {
			Tree_Rabin rabin = null;
			Tree_Streett streett = null;

			if (_options.automata == Options_LTL2DRA.AutomataType.RABIN ||
					_options.automata == Options_LTL2DRA.AutomataType.RABIN_AND_STREETT) {
				rabin = new Tree_Rabin(_ltl, _apset, _options, _sched);
			}

			if (_options.automata == Options_LTL2DRA.AutomataType.STREETT ||
					_options.automata == Options_LTL2DRA.AutomataType.RABIN_AND_STREETT) {
				streett = new Tree_Streett(_ltl.negate().simplify(), _apset, _options, _sched);
			}

			if (rabin != null && streett != null) {
				int rabin_est = rabin.guestimate();
				int streett_est = streett.guestimate();

				if (_options.verbose_scheduler) {
					System.err.println("NBA-Estimates: Rabin: " + rabin_est + " Streett: " + streett_est);
				}

				if (rabin_est <= streett_est) {
					addChild(rabin);
					addChild(streett);
				} else {
					addChild(streett);
					addChild(rabin);
				}
			} else {
				if (rabin != null)
					addChild(rabin);
				if (streett != null) 
					addChild(streett);
			}


			/*			if (_options.opt_safra.stutter) {
				StutterSensitivenessInformation::ptr stutter_information(new StutterSensitivenessInformation);
				stutter_information->checkLTL(_ltl);

				if (!stutter_information->isCompletelyInsensitive() && _options.opt_safra.partial_stutter_check) {
					NBA_ptr nba, complement_nba;
					if (rabin) {
						nba=rabin->getNBA();
					} else if (streett) {
						nba=streett->getNBA();
					}

					if (rabin && streett) {
						complement_nba=streett->getNBA();
					}

					if (!nba) {
						stutter_information->checkPartial(*_ltl, _sched.getLTL2DRA());
					} else if (!complement_nba) {
						stutter_information->checkPartial(*nba, *_ltl->negate()->toPNF(), _sched.getLTL2DRA());
					} else {
						stutter_information->checkNBAs(*nba, *complement_nba);
					}
				}

				if (rabin) {
					rabin->setStutterInformation(stutter_information);
				}
				if (streett) {
					streett->setStutterInformation(stutter_information);
				}
			}
			 */
		}
	}

	/** A building block for the calculation of a Rabin automaton 
	 * (via Safra, Scheck or Union) */
	public static class Tree_Rabin extends Tree {

		private Tree_Safra _tree_normal;
		private Tree_Union _tree_union;

		// private StutterSensitivenessInformation::ptr _stutter_information;


		/**
		 * Constructor 
		 * @param ltl The LTL formula
		 * @param options the LTL2DSTAR options
		 * @param sched a reference back to the scheduler 
		 */
		public Tree_Rabin(SimpleLTL ltl, APSet apset, Options_LTL2DRA options, Scheduler sched) {
			super(ltl, apset, options, sched);
			_tree_normal = null;
			_tree_union = null;
			generateTree();
		}

		/** Estimate the size of the automaton (use the estimate of Safra's
		 * building block ) */
		public int guestimate() {
			if (_tree_normal != null) {
				return _tree_normal.guestimate();
			}
			return 0;
		}

		/** Hook after calculation */
		public void hook_after_calculate() {
			if (_tree_normal != null && _sched.flagStatNBA()) {
				_comment = _comment + "+NBAstd=" + guestimate();
			}
		}

		/** Generate the tree */
		public void generateTree() {
			/* if (_options.scheck_path!="") {
				if (LTL2DSTAR_Tree_Scheck::worksWith(*_ltl, _options.verbose_scheduler)) {
	  				addChild(new LTL2DSTAR_Tree_Scheck(_ltl, _options, _sched));
				}
				// add stuff for path. check here
      		}*/
			if (_options.allow_union &&	_ltl.kind == SimpleLTL.LTLType.OR) {
				_tree_union = new Tree_Union(_ltl, _apset, _options, _sched);
				addChild(_tree_union);
			}

			//	if (!((_options.only_union && _options.allow_union) || (_options.only_safety && _options.safety))) {
			if (!((_options.only_union && _options.allow_union))) {
				_tree_normal = new Tree_Safra(_ltl, _apset, _options, _sched);
				addChild(_tree_normal);
			}
		}

		public NBA getNBA() {
			if (_tree_normal != null) {
				return _tree_normal.getNBA();
			}
			return null;
		}

		/* public void setStutterInformation(StutterSensitivenessInformation::ptr stutter_information) {
			_stutter_information=stutter_information;
			if (_tree_normal) {
				_tree_normal->setStutterInformation(_stutter_information);
			}

			if (_tree_union) {
				_tree_union->setStutterInformation(_stutter_information);
			}
		}*/
	}


	/** Building block for the translation from LTL to DRA using Safra's algorithm */
	public static class Tree_Safra extends Tree {

		private NBA _nba;
		// private StutterSensitivenessInformation::ptr _stutter_information;

		/**
		 * Constructor 
		 * @param ltl The LTL formula
		 * @param options the LTL2DSTAR options
		 * @param sched a reference back to the scheduler 
		 */
		public Tree_Safra(SimpleLTL ltl, APSet apset, Options_LTL2DRA options, Scheduler sched) {
			super(ltl, apset, options, sched);
			generateTree();
		}

		/** Generate the tree */
		public void generateTree() {}

		/** Translate LTL -> NBA */
		public void generateNBA() {
			if (_nba == null) {
				try {
					_nba = _ltl.toNBA(_apset);
				}
				catch (PrismException e) {
					_nba = null;
					System.err.println("Scheduler.generateNBA() : " + e);
				}
			}
		}

		public NBA getNBA() {
			generateNBA();
			return _nba;
		}

		/** Estimate the size of the DRA (returns the size of the NBA) */
		public int guestimate() {
			generateNBA();
			if (_nba != null) {
				return _nba.size();
			}
			return 0;
		}

		/** Translate the LTL formula to DRA using Safra's algorithm */
		public void calculate(int level, int limit) throws PrismException {
			if (_options.verbose_scheduler) {
				System.err.println("Calculate (" + level + "): " + this.getClass().getName());
				System.err.println(" Limit = " + limit);
			}

			generateNBA();

			if (_nba == null) {
				throw new PrismException("Couldn't create NBA from LTL formula");
			}
			
			// _automaton = _sched.getLTL2DRA().nba2dra(_nba, limit, _options.detailed_states,	_stutter_information);
			_automaton = _sched.getLTL2DRA().nba2dra(_nba, limit, _options.detailed_states);
			_comment = "Safra[NBA=" + _nba.size() +	"]";

			if (_options.optimizeAcceptance) {
				_automaton.optimizeAcceptanceCondition();
			}

			if (_options.bisim) {
				DRAOptimizations dra_optimizer = new DRAOptimizations();
				_automaton = dra_optimizer.optimizeBisimulation(_automaton,	false, _options.detailed_states, false);
			}
		}

		/*public void setStutterInformation(StutterSensitivenessInformation::ptr stutter_information) {
			_stutter_information=stutter_information;
		}
		 */
	}


	/** Generate DRA by using the union construction on two DRAs */
	public static class Tree_Union extends Tree {

		/**
		 * Constructor 
		 * @param ltl The LTL formula
		 * @param options the LTL2DSTAR options
		 * @param sched a reference back to the scheduler 
		 */

		public Tree_Union(SimpleLTL ltl, APSet apset, Options_LTL2DRA options, Scheduler sched) {

			super(ltl, apset, options, sched);
			_left_tree = null;
			_right_tree = null;

			_left = _ltl.left;
			_right = _ltl.right;

			generateTree();
		}

		/**
		 * Generate the tree
		 */
		public void generateTree() {
			Options_LTL2DRA rec_opt = _options.clone();
			rec_opt.recursion();
			_left_tree = new Tree_Rabin(_left, _apset, rec_opt, _sched);
			addChild(_left_tree);
			_right_tree = new Tree_Rabin(_right, _apset, rec_opt, _sched);
			addChild(_right_tree);
		}

		/**
		 * Perform union construction
		 */
		public void calculate(int level, int limit) throws PrismException {
			if (_options.verbose_scheduler) {
				System.err.println("Calculate (" + level + "): " + this.getClass().getName());
			}

			try {
				children.get(0).calculate(level + 1, limit);
				children.get(1).calculate(level + 1, limit);
			} catch (PrismException e) {
				_automaton = null;
				throw e;
			}      

			if (children.get(0)._automaton == null || children.get(1)._automaton == null) {
				return;
			}

			boolean union_trueloop = _sched.getLTL2DRA().getOptions().union_trueloop;
			/* if (_sched.getLTL2DRA().getOptions().stutter) {
				_automaton=DRA_t::calculateUnionStuttered(*children[0]->_automaton,
			 *children[1]->_automaton,
						_stutter_information,
						union_trueloop,
						_options.detailed_states);
			} else {
			*/			
			_automaton = children.get(0)._automaton.calculateUnion(children.get(1)._automaton,	union_trueloop,	_options.detailed_states);
			/*      _automaton=DRAOperations::dra_union(*children[0]->_automaton, 
			 *children[1]->_automaton,
					  union_trueloop,
					  _options.detailed_states); */
			// }
			_comment = "Union{" + children.get(0)._comment + "," +	children.get(1)._comment + "}";

			if (_options.optimizeAcceptance) {
				_automaton.optimizeAcceptanceCondition();
			}

			if (_options.bisim) {
				DRAOptimizations dra_optimizer = new DRAOptimizations();
				_automaton = dra_optimizer.optimizeBisimulation(_automaton,	false, _options.detailed_states, false);
			}
			hook_after_calculate();
		}


		/* public void setStutterInformation(StutterSensitivenessInformation::ptr stutter_information) {
			_stutter_information=stutter_information;

			StutterSensitivenessInformation::ptr 
			left_stutter_info(new StutterSensitivenessInformation(*stutter_information));
			StutterSensitivenessInformation::ptr 
			right_stutter_info(new StutterSensitivenessInformation(*stutter_information));

			if (!stutter_information->isCompletelyInsensitive()) {
				left_stutter_info->checkLTL(_left);
				right_stutter_info->checkLTL(_right);
			}

			if (!left_stutter_info->isCompletelyInsensitive()) {
				left_stutter_info->checkPartial(*_left_tree->getNBA(), 
		 *_left->negate()->toPNF(), 
						_sched.getLTL2DRA());
			}

			if (!right_stutter_info->isCompletelyInsensitive()) {
				right_stutter_info->checkPartial(*_right_tree->getNBA(), 
		 *_right->negate()->toPNF(), 
						_sched.getLTL2DRA());
			}

			_left_tree->setStutterInformation(left_stutter_info);
			_right_tree->setStutterInformation(right_stutter_info); 
		}
		 */

		private Tree_Rabin _left_tree;
		private Tree_Rabin _right_tree;

		private SimpleLTL _left;
		private SimpleLTL _right;

		// private StutterSensitivenessInformation::ptr _stutter_information;
	}


	/**
	 * Generate Streett automaton by calculating the Rabin automaton
	 * for the negated formula
	 */  
	public static class Tree_Streett extends Tree {

		private Tree_Rabin _tree_rabin;
		// private StutterSensitivenessInformation::ptr _stutter_information;

		/**
		 * Constructor 
		 * @param ltl The LTL formula
		 * @param options the LTL2DSTAR options
		 * @param sched a reference back to the scheduler 
		 */

		public Tree_Streett(SimpleLTL ltl, APSet apset, Options_LTL2DRA options, Scheduler sched) {
			super(ltl, apset, options, sched);
			generateTree();
		}

		/** Estimate automaton size (use estimate of Rabin building block) */
		public int guestimate() {
			if (children.get(0) != null) {
				return children.get(0).guestimate();
			}
			return 0;
		}

		public NBA getNBA() {
			if (_tree_rabin != null) {
				return _tree_rabin.getNBA();
			}
			return null;
		}

		/** Generate tree */
		public void generateTree() {
			Options_LTL2DRA opt = _options;
			opt.automata = Options_LTL2DRA.AutomataType.RABIN;
			// opt.scheck_path=""; // disable scheck
			_tree_rabin = new Tree_Rabin(_ltl, _apset, opt, _sched);
			addChild(_tree_rabin);
		}    

		/** Calculate */
		public void calculate(int level, int limit) throws PrismException {
			if (_options.verbose_scheduler) {
				System.err.println("Calculate (" + level + "): " + this.getClass().getName());
			}

			try {
				children.get(0).calculate(level, limit);
			} catch (PrismException e) {
				_automaton = null;
				throw e;
			}

			_automaton = children.get(0)._automaton;
			_comment = "Streett{" +	children.get(0)._comment + "}";

			if (_automaton != null) {
				_automaton.considerAsStreett(true);
			}

			hook_after_calculate();
		}

		/* void setStutterInformation(StutterSensitivenessInformation::ptr stutter_information) {
			_stutter_information=stutter_information;
			_tree_rabin->setStutterInformation(stutter_information);
		}*/

	}

	/** Constructor
	 * @param ltl2dra the wrapper for LTL->NBA and NBA->DRA 
	 * @param opt_limits use limiting?
	 * @param alpha the limiting factor 
	 */
	public Scheduler(LTL2DRA ltl2dra, boolean opt_limits, double alpha) {
		_ltl2dra = ltl2dra;
		_opt_limits = opt_limits;
		_alpha = alpha;
		_stat_NBA = false;
	}



	/** Calculate the new limit using factor alpha (returns 0 if no limit) */
	public int calcLimit(int limit) {
		if (limit == 0) {return limit;}
		if (flagOptLimits()) {
			double new_limit = (limit * _alpha) + 1.0;
			if (new_limit > Integer.MAX_VALUE) {
				limit = 0;
			} else {
				limit = (int)new_limit;
			}
		}
		return limit;
	}

	/** 
	 * Generate a DRA/DSA for the LTL formula 
	 */
	public DRA calculate(SimpleLTL ltl, APSet apset, Options_LTL2DRA ltl_opt) throws PrismException {

		if (ltl_opt.verbose_scheduler) {
			System.err.println(ltl);
		}

		Tree root = new Tree_Start(ltl, apset, ltl_opt, this);

		if (ltl_opt.verbose_scheduler) {
			root.printTree(System.err, 0);
		}

		root.calculate(0,0);

		DRA result = root._automaton;
		if (result != null) {
			result.setComment(root._comment + getTimingInformation());
		}
		return result;
	}

	public String getTimingInformation() {
		/* if (StutterSensitivenessInformation::hasTimekeeping()) {
			unsigned long ms=StutterSensitivenessInformation::getTimeKeeper().getElapsedMilliseconds();
			return " TIME(stuttercheck)=:"+ 
			boost::lexical_cast<std::string>(ms)+
			":";
		} else {
		 */
		return "";
		// }
	}

	/** Get the LTL2DRA wrapper class */
	public LTL2DRA getLTL2DRA() {
		return _ltl2dra;
	}

	public boolean flagOptLimits() {return _opt_limits;}

	/** Get the state of the StatNBA flag */
	public boolean flagStatNBA() {return _stat_NBA;}

	/** Set the value of the StatNBA flag */
	public void flagStatNBA(boolean value) {_stat_NBA=value;}
	
}
