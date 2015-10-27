package trading.common;

import trading.INegotiationAgent;
import trading.INegotiationGoal;
import trading.buyer.BuyerBDI.PurchaseItem;
import trading.seller.SellerBDI.SellItem;
import jadex.bdiv3.runtime.IBeliefListener;
import jadex.bdiv3.runtime.IGoal;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.gui.SGUI;
import jadex.commons.gui.future.SwingDefaultResultListener;
import jadex.commons.gui.future.SwingResultListener;
import jadex.commons.transformation.annotations.Classname;
import jadex.micro.IPojoMicroAgent;
import jadex.micro.annotation.Binding;
import jadex.rules.eca.ChangeInfo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *  The gui allows to add and delete buy or sell orders and shows open and
 *  finished orders.
 */
@SuppressWarnings("serial")
public class GuiPanel extends JPanel
{
	//-------- attributes --------

	private String itemlabel;
//	private String goalname;
	private String addorderlabel;
	private IExternalAccess agent;
	@SuppressWarnings("rawtypes")
	private List orders = new ArrayList();
	private JTable table;
	private DefaultTableModel detailsdm; 
	private AbstractTableModel items = new AbstractTableModel()
	{
		//initialize the total number of columns in the table
		private final int numberOfColumns = 8;
		
		public int getRowCount()
		{
			return orders.size();
		}

		public int getColumnCount()
		{
			return this.numberOfColumns;
		}

		public String getColumnName(int column)
		{
			switch(column)
			{
				case 0:
					return "Name";
				case 1:
					return "Start Price";
				case 2:
					return "Limit";
				case 3:
					return "Deadline";
				case 4:
					return "Execution Price";
				case 5:
					return "Execution Date";
				case 6:
					return "State";
				case 7:
					return "Negotiation Strategy";
				default:
					return "";
			}
		}

		public boolean isCellEditable(int row, int column)
		{
			return false;
		}

		public Object getValueAt(int row, int column)
		{
			Object value = null;
			Order order = (Order)orders.get(row);
			if(column == 0)
			{
				value = order.getName();
			}
			else if(column == 1)
			{
				value = new Integer(order.getStartPrice());
			}
			else if(column == 2)
			{
				value = new Integer(order.getLimit());
			}
			else if(column == 3)
			{
				value = order.getDeadline();
			}
			else if(column == 4)
			{
				value = order.getExecutionPrice();
			}
			else if(column == 5)
			{
				value = order.getExecutionDate();
			}
			else if(column == 6)
			{
				value = order.getState();
			}
			else if(column == 7)
			{
				value = order.getNegotiationStrategy();
			}
			return value;
		}
	};
	private DateFormat dformat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	//-------- constructors --------

	/**
	 *  Shows the gui, and updates it when beliefs change.
	 */
	public GuiPanel(final IExternalAccess agent)//, final boolean buy)
	{
		setLayout(new BorderLayout());
		
		this.agent = agent;
		final boolean buy = isBuyer(agent);
		
		if(buy)
		{
			itemlabel = " Item to buy ";
//			goalname = "purchase_item";
			addorderlabel = "Add new purchase order";
		}
		else
		{
			itemlabel = " Item to sell ";
//			goalname = "sell_item";
			addorderlabel = "Add new sell order";
		}

		JPanel itempanel = new JPanel(new BorderLayout());
		itempanel.setBorder(new TitledBorder(new EtchedBorder(), itemlabel));

		table = new JTable(items);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{
			public Component getTableCellRendererComponent(JTable table,
				Object value, boolean selected, boolean focus, int row, int column)
			{
				Component comp = super.getTableCellRendererComponent(table,
					value, selected, focus, row, column);
				setOpaque(true);
				if(column == 0)
				{
					setHorizontalAlignment(LEFT);
				}
				else
				{
					setHorizontalAlignment(RIGHT);
				}
				if(!selected)
				{
					Object state = items.getValueAt(row, 6);
					if(Order.DONE.equals(state))
					{
						comp.setBackground(new Color(211, 255, 156));
					}
					else if(Order.FAILED.equals(state))
					{
						comp.setBackground(new Color(255, 211, 156));
					}
					else
					{
						comp.setBackground(table.getBackground());
					}
				}
				if(value instanceof Date)
				{
					setValue(dformat.format(value));
				}
				return comp;
			}
		});
		table.setPreferredScrollableViewportSize(new Dimension(600, 120));
		
		JScrollPane scroll = new JScrollPane(table);
		itempanel.add(BorderLayout.CENTER, scroll);
		
		detailsdm = new DefaultTableModel(new String[]{"Negotiation Details"}, 0);
		JTable details = new JTable(detailsdm);
		details.setPreferredScrollableViewportSize(new Dimension(600, 120));
		
		JPanel dep = new JPanel(new BorderLayout());
		dep.add(BorderLayout.CENTER, new JScrollPane(details));
	
		JPanel south = new JPanel();
		// south.setBorder(new TitledBorder(new EtchedBorder(), " Control "));
		JButton add = new JButton("Add");
		final JButton remove = new JButton("Remove");
		final JButton edit = new JButton("Edit");
		add.setMinimumSize(remove.getMinimumSize());
		add.setPreferredSize(remove.getPreferredSize());
		edit.setMinimumSize(remove.getMinimumSize());
		edit.setPreferredSize(remove.getPreferredSize());
		south.add(add);
		south.add(remove);
		south.add(edit);
		remove.setEnabled(false);
		edit.setEnabled(false);
		
		JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitter.add(itempanel);
		splitter.add(dep);
		splitter.setOneTouchExpandable(true);
		//splitter.setDividerLocation();
		
		add(BorderLayout.CENTER, splitter);
		add(BorderLayout.SOUTH, south);
		
		//To refresh the item panel when updated/ edited
		agent.scheduleStep(new IComponentStep<Void>()
		{
			@Classname("refresh")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INegotiationAgent ag = (INegotiationAgent)((IPojoMicroAgent)ia).getPojoAgent();
				ag.getAgent().addBeliefListener("orders", new IBeliefListener<Object>()
				{
					public void factRemoved(ChangeInfo<Object> info)
					{
						refresh();
					}
					
					public void factChanged(ChangeInfo<Object> info)
					{
						refresh();
					}
					
					public void factAdded(ChangeInfo<Object> info)
					{
						refresh();
					}
					
					public void beliefChanged(ChangeInfo<Object> info)
					{
						refresh();
					}
				});
				return IFuture.DONE;
			}
		});
		 
		//to refresh the  detail panel when the item panel is updated/ edited
		agent.scheduleStep(new IComponentStep<Void>()
		{
			@Classname("refreshDetails")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INegotiationAgent ag = (INegotiationAgent)((IPojoMicroAgent)ia).getPojoAgent();
				ag.getAgent().addBeliefListener("reports", new IBeliefListener<Object>()
				{
					public void factRemoved(ChangeInfo<Object> info)
					{
						refreshDetails();
					}
					
					public void factChanged(ChangeInfo<Object> info)
					{
						refreshDetails();
					}
					
					public void factAdded(ChangeInfo<Object> info)
					{
						refreshDetails();
					}
					
					public void beliefChanged(ChangeInfo<Object> info)
					{
						refreshDetails();
					}
				});
				return IFuture.DONE;
			}
		});
		
		table.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				refreshDetails();
			}
		} );
		
		//when the new order is added
		final InputDialog dia = new InputDialog(buy);		
		add.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				agent.scheduleStep(new IComponentStep<Void>()
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					public IFuture<Void> execute(IInternalAccess ia)
					{
						ia.getServiceContainer().searchService(IClockService.class, Binding.SCOPE_PLATFORM)
							.addResultListener(new SwingDefaultResultListener(GuiPanel.this)
						{
							public void customResultAvailable(Object result)
							{
								IClockService cs = (IClockService)result;
								while(dia.requestInput(cs.getTime()))
								{
									try
									{
										String name = dia.name.getText();
										int limit = Integer.parseInt(dia.limit.getText());
										int start = Integer.parseInt(dia.start.getText());
										Date deadline = dformat.parse(dia.deadline.getText());
										String strategy = dia.strategy.getSelectedItem().toString();
										final Order order = new Order(name, deadline, start, limit, buy, cs,strategy);
										
										agent.scheduleStep(new IComponentStep<Void>()
										{
											@Classname("add")
											public IFuture<Void> execute(IInternalAccess ia)
											{
												INegotiationAgent ag = (INegotiationAgent)((IPojoMicroAgent)ia).getPojoAgent();
												ag.createGoal(order); 
												return IFuture.DONE;
											}
										});
	//									agent.createGoal(goalname).addResultListener(new DefaultResultListener()
	//									{
	//										public void resultAvailable(Object source, Object result)
	//										{
	//											IEAGoal purchase = (IEAGoal)result;
	//											purchase.setParameterValue("order", order);
	//											agent.dispatchTopLevelGoal(purchase);
	//										}
	//									});
										orders.add(order);
										items.fireTableDataChanged();
										break;
									}
									catch(NumberFormatException e1)
									{
										JOptionPane.showMessageDialog(GuiPanel.this, "Price limit must be integer.", "Input error", JOptionPane.ERROR_MESSAGE);
									}
									catch(ParseException e1)
									{
										JOptionPane.showMessageDialog(GuiPanel.this, "Wrong date format, use YYYY/MM/DD hh:mm.", "Input error", JOptionPane.ERROR_MESSAGE);
									}
								}
							}
						});
						return IFuture.DONE;
					}
				});
				
			}
		}); //end of the new order adding operation

		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{

			public void valueChanged(ListSelectionEvent e)
			{
				boolean selected = table.getSelectedRow() >= 0;
				remove.setEnabled(selected);
				edit.setEnabled(selected);
			}
		});

		//when the order is removed
		remove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int row = table.getSelectedRow();
				if(row >= 0 && row < orders.size())
				{
					final Order order = (Order)orders.remove(row);
					items.fireTableRowsDeleted(row, row);
					
					agent.scheduleStep(new IComponentStep<Void>()
					{
						@Classname("remove")
						public IFuture<Void> execute(IInternalAccess ia)
						{
							INegotiationAgent ag = (INegotiationAgent)((IPojoMicroAgent)ia).getPojoAgent();
							Collection<INegotiationGoal> gs = ag.getGoals();
							for(INegotiationGoal g: gs)
							{
								 Order or = g.getOrder();
								if(order.equals(or))
								{
									or.setState(Order.FAILED);
									ag.getAgent().dropGoal(g);
									break;
								}
							}
							return IFuture.DONE;
						}
					});
				}
			}
		}); // end of the order remove operation

		
		//when the order is edited
		final InputDialog edit_dialog = new InputDialog(buy);
		edit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				agent.scheduleStep(new IComponentStep<Void>()
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					public IFuture<Void> execute(IInternalAccess ia)
					{
						ia.getServiceContainer().searchService(IClockService.class, Binding.SCOPE_PLATFORM)
							.addResultListener(new SwingDefaultResultListener(GuiPanel.this)
						{
							public void customResultAvailable(Object result)
							{
								IClockService cs = (IClockService)result;
						
								int row = table.getSelectedRow();
								if(row >= 0 && row < orders.size())
								{
									final Order order = (Order)orders.get(row);
									edit_dialog.name.setText(order.getName());
									edit_dialog.limit.setText(Integer.toString(order.getLimit()));
									edit_dialog.start.setText(Integer.toString(order.getStartPrice()));
									edit_dialog.deadline.setText(dformat.format(order.getDeadline()));
									edit_dialog.strategy.setSelectedItem(order.getNegotiationStrategy());
				
									while(edit_dialog.requestInput(cs.getTime()))
									{
										try
										{
											String name = edit_dialog.name.getText();
											int limit = Integer.parseInt(edit_dialog.limit.getText());
											int start = Integer.parseInt(edit_dialog.start.getText());
											Date deadline = dformat.parse(edit_dialog.deadline.getText());
											String strategy = edit_dialog.strategy.getSelectedItem().toString();
											order.setName(name);
											order.setLimit(limit);
											order.setStartPrice(start);
											order.setDeadline(deadline);
											order.setNegotiationStrategy(strategy);
											items.fireTableDataChanged();
											
											agent.scheduleStep(new IComponentStep<Void>()
											{
												@Classname("drop")
												public IFuture<Void> execute(IInternalAccess ia)
												{
													INegotiationAgent ag = (INegotiationAgent)((IPojoMicroAgent)ia).getPojoAgent();
													Collection<INegotiationGoal> goals = ag.getGoals();
													
													for(INegotiationGoal goal: goals)
													{
														if(goal.getOrder().equals(order))
														{
															ag.getAgent().dropGoal(goal);
														}
													}
													
													ag.createGoal(order);
													return IFuture.DONE;
												}
											});
											break;
										}
										catch(NumberFormatException e1)
										{
											JOptionPane.showMessageDialog(GuiPanel.this, "Price limit must be integer.", "Input error", JOptionPane.ERROR_MESSAGE);
										}
										catch(ParseException e1)
										{
											JOptionPane.showMessageDialog(GuiPanel.this, "Wrong date format, use YYYY/MM/DD hh:mm.", "Input error", JOptionPane.ERROR_MESSAGE);
										}
									}
								}
							}
						});
						return IFuture.DONE;
					}
				});
				
			}
		});// end of the order edit operation
		 
		//initial refresh to the item table
		refresh(); 
		
		//initial refresh to the order detail table
		refreshDetails();
		
	}//end of constructor
 
	
	//-------- methods --------

	/**
	 * Method to be called when goals may have changed.
	 */
	public void refresh()
	{
		agent.scheduleStep(new IComponentStep<Void>()
		{
			@Classname("ref")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INegotiationAgent ag = (INegotiationAgent)((IPojoMicroAgent)ia).getPojoAgent();

				final List<Order> aorders = ag.getOrders();
				SwingUtilities.invokeLater(new Runnable()
				{
					@SuppressWarnings("unchecked")
					public void run()
					{
						for(Order order: aorders)
						{
							if(!orders.contains(order))
							{
								orders.add(order);
							}
						}
						items.fireTableDataChanged();
					}
				});
				return IFuture.DONE;
			}
		});
	}
	
	/**
	 *  Refresh the details panel.
	 */
	@SuppressWarnings("unused")
	public void refreshDetails()
	{
		// initialize the cell as the table selected row number
		int cell = table.getSelectedRow(); 
		
		//if table row has not selected and orders are available in the list
		if(cell == -1 && orders.size() > 0){ 
			//set the first row as selected
			cell = 0;
		}
		
		//if the cell has zero or positive value
		if( cell >= 0 )
		{ 	
			 
			final Order order = (Order)orders.get(cell);
			agent.scheduleStep(new IComponentStep<Void>()
			{
				@Classname("refD")
				public IFuture<Void> execute(IInternalAccess ia)
				{
					INegotiationAgent ag = (INegotiationAgent)((IPojoMicroAgent)ia).getPojoAgent();
					
					final List<NegotiationReport> reps = ag.getReports(order);
					
					//sort the reports according to the time the order generated
					Collections.sort(reps, new Comparator<NegotiationReport>()
					{
						public int compare(NegotiationReport o1, NegotiationReport o2)
						{
							return o1.getTime()>o2.getTime()? 1: -1;
						}
					});
					
					// update the detail table
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							while(detailsdm.getRowCount()>0)
								detailsdm.removeRow(0);
							for(NegotiationReport rep: reps)
							{
								detailsdm.addRow(new Object[]{rep});
								//System.out.println(""+i+res.get(i));
							}
						}
					});
					
					//return successfully done
					return IFuture.DONE;
				}
			}); 
		}
	}	
	 
	/**
	 *  write negotiation history to the excel file.
	 */	
	public void writeDetails()
	{
		agent.scheduleStep(new IComponentStep<Void>()
				{
					@Classname("ref")
					public IFuture<Void> execute(IInternalAccess ia)
					{
						INegotiationAgent ag = (INegotiationAgent)((IPojoMicroAgent)ia).getPojoAgent();

						final List<Order> aorders = ag.getOrders();
						SwingUtilities.invokeLater(new Runnable()
						{
							@SuppressWarnings("unchecked")
							public void run()
							{
								/*for(Order order: aorders)
								{
									if(!orders.contains(order))
									{
										orders.add(order);
									}
								}
								items.fireTableDataChanged();*/
							}
						});
						return IFuture.DONE;
					}
				});	 
	}
	
	

	/**
	 *  Get the frame.
	 */
	public Frame getFrame()
	{
		Container parent = this.getParent();
		while(parent!=null && !(parent instanceof Frame))
			parent = parent.getParent();
		return (Frame)parent;
	}
	
	//---------------- inner classes --------------------------

	/**
	 *  The input dialog.
	 */
	private class InputDialog extends JDialog
	{
		@SuppressWarnings("rawtypes")
		private JComboBox orders = new JComboBox();
		private JTextField name = new JTextField(20);
		private JTextField limit = new JTextField(20);
		private JTextField start = new JTextField(20);
		private JTextField deadline = new JTextField(20);
		private JComboBox strategy = new JComboBox();
		private boolean aborted;
		private Exception e;

		InputDialog(final boolean buy)
		{
			super(getFrame(), addorderlabel, true);

			// Add some default entries for easy testing of the gui.
			// These orders are not added to the agent (see manager.agent.xml).
			agent.scheduleStep(new IComponentStep<Void>()
			{
				public IFuture<Void> execute(IInternalAccess ia)
				{
					ia.getServiceContainer().searchService(IClockService.class, Binding.SCOPE_PLATFORM)
						.addResultListener(new SwingResultListener<IClockService>(new IResultListener<IClockService>()
					{
						@SuppressWarnings("unchecked")
						public void resultAvailable(IClockService clock)
						{
							try
							{
								if(buy)
								{
									orders.addItem(new Order("All about agents", null, 100, 120, buy, clock,"strategy-1"));
									orders.addItem(new Order("All about web services", null, 40, 60, buy, clock,"strategy-2"));
									orders.addItem(new Order("Harry Potter", null, 5, 10, buy, clock,"strategy-3"));
									orders.addItem(new Order("Agents in the real world", null, 30, 65, buy, clock,"strategy-4"));
								}
								else
								{
									orders.addItem(new Order("All about agents", null, 130, 110, buy, clock,"strategy-1"));
									orders.addItem(new Order("All about web services", null, 50, 30, buy, clock,"strategy-2"));
									orders.addItem(new Order("Harry Potter", null, 15, 9, buy, clock,"strategy-3"));
									orders.addItem(new Order("Agents in the real world", null, 100, 60, buy, clock,"strategy-4"));
								}
								
								strategy.addItem("strategy-1");
								strategy.addItem("strategy-2");
								strategy.addItem("strategy-3");
								strategy.addItem("strategy-4");
							}
							catch(Exception e)
							{
								// happens when killed during startup
							}
							
							JPanel center = new JPanel(new GridBagLayout());
							center.setBorder(new EmptyBorder(5, 5, 5, 5));
							getContentPane().add(BorderLayout.CENTER, center);
	
							JLabel label;
							Dimension labeldim = new JLabel("Preset orders ").getPreferredSize();
							int row = 0;
							GridBagConstraints leftcons = new GridBagConstraints(0, 0, 1, 1, 0, 0,
									GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0);
							GridBagConstraints rightcons = new GridBagConstraints(1, 0, GridBagConstraints.REMAINDER, 1, 1, 0,
									GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0);
	
							leftcons.gridy = rightcons.gridy = row++;
							label = new JLabel("Preset orders");
							label.setMinimumSize(labeldim);
							label.setPreferredSize(labeldim);
							center.add(label, leftcons);
							center.add(orders, rightcons);
	
							leftcons.gridy = rightcons.gridy = row++;
							label = new JLabel("Name");
							label.setMinimumSize(labeldim);
							label.setPreferredSize(labeldim);
							center.add(label, leftcons);
							center.add(name, rightcons);
	
							leftcons.gridy = rightcons.gridy = row++;
							label = new JLabel("Start price");
							label.setMinimumSize(labeldim);
							label.setPreferredSize(labeldim);
							center.add(label, leftcons);
							center.add(start, rightcons);
	
							leftcons.gridy = rightcons.gridy = row++;
							label = new JLabel("Price limit");
							label.setMinimumSize(labeldim);
							label.setPreferredSize(labeldim);
							center.add(label, leftcons);
							center.add(limit, rightcons);
	
							leftcons.gridy = rightcons.gridy = row++;
							label = new JLabel("Deadline");
							label.setMinimumSize(labeldim);
							label.setPreferredSize(labeldim);
							center.add(label, leftcons);
							center.add(deadline, rightcons);
							
							leftcons.gridy = rightcons.gridy = row++;
							label = new JLabel("Negotiation Strategy");
							label.setMinimumSize(labeldim);
							label.setPreferredSize(labeldim);
							center.add(label, leftcons);
							center.add(strategy, rightcons);
	
	
							JPanel south = new JPanel();
							// south.setBorder(new TitledBorder(new EtchedBorder(), " Control "));
							getContentPane().add(BorderLayout.SOUTH, south);
	
							JButton ok = new JButton("Ok");
							JButton cancel = new JButton("Cancel");
							ok.setMinimumSize(cancel.getMinimumSize());
							ok.setPreferredSize(cancel.getPreferredSize());
							south.add(ok);
							south.add(cancel);
	
							ok.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									aborted = false;
									setVisible(false);
								}
							});
							cancel.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									setVisible(false);
								}
							});
	
							orders.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									Order order = (Order)orders.getSelectedItem();
									name.setText(order.getName());
									limit.setText("" + order.getLimit());
									start.setText("" + order.getStartPrice());
									strategy.setSelectedItem(order.getNegotiationStrategy());
								}
							});
						}
						
						public void exceptionOccurred(Exception exception)
						{
							e	= exception;
						}
					}));
					return IFuture.DONE;
				}
			});
			
		}

		public boolean requestInput(long currenttime)
		{
			if(e!=null)
			{
				throw new RuntimeException(e);
			}
			else
			{
				this.deadline.setText(dformat.format(new Date(currenttime + 300000)));
				this.aborted = true;
				this.pack();
				this.setLocation(SGUI.calculateMiddlePosition(getFrame(), this));
				this.setVisible(true);
				return !aborted;
			}
		}
	}
	
	/**
	 *  Test if agent is a buyer.
	 */
	public static boolean isBuyer(IExternalAccess agent)
	{
		return agent.getModel().getName().indexOf("Buyer")!=-1;
	}
}

/**
 *  The dialog for showing details about negotiations.
 * /
class NegotiationDetailsDialog extends JDialog
{
	/**
	 *  Create a new dialog. 
	 *  @param owner The owner.
	 * /
	public NegotiationDetailsDialog(Frame owner, List details)
	{
		super(owner);
		DefaultTableModel tadata = new DefaultTableModel(new String[]{"Negotiation Details"}, 0);
		JTable table = new JTable(tadata);
		for(int i=0; i<details.size(); i++)
			tadata.addRow(new Object[]{details.get(i)});
		
		JButton ok = new JButton("ok");
		ok.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				NegotiationDetailsDialog.this.setVisible(false);
				NegotiationDetailsDialog.this.dispose();
			}
		});
		JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
		south.add(ok);

		this.getContentPane().add("Center", table);
		this.getContentPane().add("South", south);
		this.setSize(400,200);
		this.setLocation(SGUI.calculateMiddlePosition(this));
		this.setVisible(true);
	}
}*/