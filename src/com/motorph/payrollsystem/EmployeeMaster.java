package com.motorph.payrollsystem;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import javax.swing.JTabbedPane;

public class EmployeeMaster extends JFrame {

	private static final long serialVersionUID = 1L;
	private JTable table;
	protected List<EmployeeDetails> employees = new ArrayList<>();
	private JTable table_1;
	
	private List<LeaveRepository.Row> leaveRows = new ArrayList<>();   // row i of table_1 = leaveRows.get(i)
	private JButton btnApprove, btnReject, btnEdit, btnDelete_1;
	/**
	 * Launch the application.
	 */
//	public static void main(String[] args) {
//		EmployeeMaster frame = new EmployeeMaster();
//		frame.viewEmployeeMaster();
//		
//	}
	
	public void viewEmployeeMaster() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EmployeeMaster frame = new EmployeeMaster();
					frame.loadEmployees();
					frame.loadLeaveApplications();
					frame.setVisible(true);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	
	public void loadEmployees() {
		try {
			SessionManager.requireRole("admin");      // backend check; also writes an audit entry if denied
			employees = EmployeeRepository.findAll();
		} catch (SecurityException | SupabaseClient.SupabaseException ex) {
			JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			employees = new ArrayList<>();
		}

		// Populate JTable with data
		Object[][] data = new Object[employees.size()][7];
		for (int j = 0; j < employees.size(); j++) {
			EmployeeDetails e = employees.get(j);
			data[j][0] = e.employeeNumber;
			data[j][1] = e.lname;
			data[j][2] = e.fname;
			data[j][3] = e.sssNumber;
			data[j][4] = e.philHealthNumber;
			data[j][5] = e.tinNumber;
			data[j][6] = e.pagIbigNumber;
		}
		table.setModel(new DefaultTableModel(
		    data,
		    new String[] {
		        "Employee No.", "Last Name", "First Name", "SSS No.", "PhilHealth No.", "TIN", "Pagibig No."
		    }
		));
		table.getColumnModel().getColumn(4).setPreferredWidth(84);
	}


	/**
	 * Create the frame.
	 */
	public EmployeeMaster() {
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
    	    public void windowClosing(WindowEvent e) {
    	        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
    	        if (confirm == JOptionPane.YES_OPTION) {
    	            // Close the application
    	            System.exit(0);
    	        }
    	    }
    	});
		setSize(789,429);
		setLocationRelativeTo(null);
		getContentPane().setLayout(null);
		// Sets icon image for the application
    	ImageIcon icon = new ImageIcon("MOTORPH.png");
    	setIconImage(icon.getImage());
		
		JButton btnBack = new JButton("Exit");
		btnBack.setBounds(687, 350, 64, 23);
		btnBack.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
	        	dispose();
	        	PayrollSystem menu = new PayrollSystem();
	        	menu.displayPayrollMenu();
				
			}
		});
		getContentPane().add(btnBack);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(10, 61, 753, 278);
		getContentPane().add(tabbedPane);
		
		JPanel panel = new JPanel();
		tabbedPane.addTab("Employee Records", null, panel, null);
		panel.setLayout(null);
				
				JScrollPane scrollPane = new JScrollPane();
				scrollPane.setBounds(10, 39, 728, 166);
				panel.add(scrollPane);
				scrollPane.setFont(new Font("Tahoma", Font.BOLD, 11));
				
				table = new JTable();
				table.setDefaultEditor(Object.class, null);
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				scrollPane.setViewportView(table);
				
				JButton btnView = new JButton("View >>");
				btnView.setBounds(20, 216, 82, 23);
				panel.add(btnView);
				btnView.setEnabled(false);
				
				JButton btnDelete = new JButton("Delete");
				btnDelete.setBounds(108, 216, 82, 23);
				panel.add(btnDelete);
				btnDelete.setEnabled(false);
				
				JPanel panel_1 = new JPanel();
				tabbedPane.addTab("Employee Leave", null, panel_1, null);
				panel_1.setLayout(null);
				
				JScrollPane scrollPane_1 = new JScrollPane();
				scrollPane_1.setBounds(10, 39, 728, 166);
				panel_1.add(scrollPane_1);
				
				btnApprove = new JButton("Approve");
				btnApprove.setEnabled(false);
				btnApprove.setFocusable(false);
				btnApprove.setBounds(20, 216, 82, 23);
				btnApprove.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						int selectedRow = table_1.getSelectedRow();
						if (selectedRow == -1) {
							return;
						}
						approve(selectedRow);
						loadLeaveApplications();
					}
				});
				panel_1.add(btnApprove);
				
				btnReject = new JButton("Reject");
				btnReject.setEnabled(false);
				btnReject.setFocusable(false);
				btnReject.setBounds(108, 216, 82, 23);
				btnReject.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						int selectedRow = table_1.getSelectedRow();
						if (selectedRow == -1) {
							return;
						}
						reject(selectedRow);
						loadLeaveApplications();
						
					}
				});
				panel_1.add(btnReject);
				
				table_1 = new JTable();
				table_1.setDefaultEditor(Object.class, null);
				table_1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				table_1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				    public void valueChanged(ListSelectionEvent event) {
				        int selectedRow = table_1.getSelectedRow();
				        if (selectedRow != -1) { // If a row is selected
				            String leaveStatus = (String) table_1.getValueAt(selectedRow, 4);
				            String employeeNumber = (String) table_1.getValueAt(selectedRow, 0);
				            if (!String.valueOf(SessionManager.employeeNumber()).equals(employeeNumber)) btnDelete_1.setEnabled(true);
				            else btnDelete_1.setEnabled(false); // Yezzz chaos all aroundd >:-D
				            if (leaveStatus.equals("Applied")) {
				            	btnEdit.setEnabled(false);
				            	if (!String.valueOf(SessionManager.employeeNumber()).equals(employeeNumber)) {
				                btnApprove.setEnabled(true);
				                btnReject.setEnabled(true);
				                } else {
				                	btnApprove.setEnabled(false);
					                btnReject.setEnabled(false);
				                }
				            } else if (leaveStatus.equals("Approved") || leaveStatus.equals("Rejected")) {
				            	btnEdit.setEnabled(true);
				            	btnApprove.setEnabled(false);
				                btnReject.setEnabled(false);
				            } else {
				                btnApprove.setEnabled(false);
				                btnReject.setEnabled(false);
				                btnEdit.setEnabled(false);
				            }
				            
				        } else {
				        	btnDelete_1.setEnabled(false);
				        }
				    }
				});
				scrollPane_1.setViewportView(table_1);		
				
				btnEdit = new JButton("Edit");
				btnEdit.setEnabled(false);
				btnEdit.setBounds(553, 216, 82, 23);
				btnEdit.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						int selectedRow = table_1.getSelectedRow();
						if (selectedRow == -1) {
							return;
						}
						String[] status = {"Approve","Reject","Cancel"};
						int answer = JOptionPane.showOptionDialog(null, "                  Select new leave status.", "Edit Leave Status", 
								JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, status, 2);
						if (answer == 0) {
							approve(selectedRow);
							
						} else if (answer == 1) {
							reject(selectedRow);
						}
						loadLeaveApplications();
					}
				});
				panel_1.add(btnEdit);
				
				btnDelete_1 = new JButton("Delete");
				btnDelete_1.setEnabled(false);
				btnDelete_1.setBounds(645, 216, 82, 23);
				btnDelete_1.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						int selectedRow = table_1.getSelectedRow();
						if (selectedRow == -1) {
							return;
						}
						delete(selectedRow);
						loadLeaveApplications();
					}
				});
				
				panel_1.add(btnDelete_1);
						
						JLabel lblMotorphEmployeeMaster = new JLabel("MotorPH Employee Master");
						lblMotorphEmployeeMaster.setBounds(294, 33, 184, 17);
						getContentPane().add(lblMotorphEmployeeMaster);
						lblMotorphEmployeeMaster.setFont(new Font("Tahoma", Font.BOLD, 14));
				
				btnDelete.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						int row = table.getSelectedRow();
						if (row < 0) {
							JOptionPane.showMessageDialog(EmployeeMaster.this, "Delete Error");
							return;
						}
						int empNo = Integer.parseInt(String.valueOf(table.getValueAt(row, 0)));   // column 0, not the row index
						String name = String.valueOf(table.getValueAt(row, 2));

						// Same protection as before (first employee), plus you can't delete yourself
						if (empNo == 10001 || empNo == SessionManager.employeeNumber()) {
							JOptionPane.showMessageDialog(EmployeeMaster.this,
									"You don't have permission to delete this employee's records.",
									"Permission Error", JOptionPane.WARNING_MESSAGE);
							return;
						}
						int confirm = JOptionPane.showConfirmDialog(null,
								"Delete " + name + "'s Employee Record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
						if (confirm != JOptionPane.YES_OPTION) {
							return;
						}

						try {
							EmployeeRepository.delete(empNo);
						} catch (SecurityException ex) {
							JOptionPane.showMessageDialog(EmployeeMaster.this, ex.getMessage(),
									"Permission Error", JOptionPane.WARNING_MESSAGE);
							return;
						} catch (SupabaseClient.SupabaseException ex) {
							String msg = ex.status == 409
									? "This employee still has linked records. Run the cascade SQL first."
									: "Delete failed: " + ex.getMessage();
							JOptionPane.showMessageDialog(EmployeeMaster.this, msg, "Delete Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						loadEmployees();
					}
				});
				
				btnView.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int i = table.getSelectedRow();
						if (i >= 0) {
							EmployeeDetailsViewer view = new EmployeeDetailsViewer();
							view.setEmployeeNumber(Integer.parseInt(String.valueOf(table.getValueAt(i, 0))));
							view.viewDetails();
							setVisible(false);
							dispose();
						} else {
							JOptionPane.showMessageDialog(EmployeeMaster.this, "View Error");
						}
					}
				});
				
				table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				    public void valueChanged(ListSelectionEvent event) {
				        int selectedRow = table.getSelectedRow();
				        if (selectedRow != -1) { // If a row is selected
				        	btnView.setEnabled(true);
				        	btnDelete.setEnabled(true);
				        } else {
				        	btnView.setEnabled(false);
				        	btnDelete.setEnabled(false);
				        }
				    }
				});
	}
	
	/** Admin only. Used by EmployeeDetailsViewer. Returns true if the save worked. */
	protected boolean saveEmployee(int empNo, String lname, String fname, String bday, String address,
			String phone, String status, String position, String supervisor) {
		try {
			EmployeeRepository.update(empNo, fname, lname, bday, address, phone, status, position, supervisor);
			return true;
		} catch (IllegalArgumentException | SecurityException | SupabaseClient.SupabaseException ex) {
			JOptionPane.showMessageDialog(null, ex.getMessage(), "Could not save changes", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
	
	public void loadLeaveApplications() {
		try {
			SessionManager.requireRole("admin");
			leaveRows = LeaveRepository.list();      // newest first; RLS decides what is visible
		} catch (SecurityException | SupabaseClient.SupabaseException ex) {
			JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			leaveRows = new ArrayList<>();
		}

		// Populate JTable with data (row i of the table = leaveRows.get(i))
		Object[][] data = new Object[leaveRows.size()][6];
		for (int j = 0; j < leaveRows.size(); j++) {
			LeaveDetails d = leaveRows.get(j).details;
			data[j][0] = d.employeeNumber;
			data[j][1] = d.leaveType;
			data[j][2] = d.startDate;
			data[j][3] = d.endDate;
			data[j][4] = d.leaveStatus;
			data[j][5] = d.leaveDescription;
		}
		table_1.setModel(new DefaultTableModel(
			data,
			new String[] {
				"Employee No.", "Leave Type", "Start Date", "End Date", "Leave Status", "Leave Description"
			}
		));
		table_1.getColumnModel().getColumn(4).setPreferredWidth(84);
	}

	/** Database id of the leave shown in the given table row, or -1 if there is none. */
	private long leaveIdAt(int selectedRow) {
		return (selectedRow >= 0 && selectedRow < leaveRows.size()) ? leaveRows.get(selectedRow).id : -1;
	}

	/** Runs a leave action and reports permission or database problems instead of failing silently. */
	private void runLeaveAction(Runnable action) {
		try {
			action.run();
		} catch (SecurityException ex) {
			JOptionPane.showMessageDialog(null, ex.getMessage(), "Permission Error", JOptionPane.WARNING_MESSAGE);
		} catch (SupabaseClient.SupabaseException ex) {
			JOptionPane.showMessageDialog(null, "Action failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void approve(int selectedRow) {
		long id = leaveIdAt(selectedRow);
		if (id < 0) return;
		int confirm = JOptionPane.showConfirmDialog(null, "Approve this leave application?", "Confirm Approval", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) return;
		runLeaveAction(() -> LeaveRepository.approve(id));
	}

	public void reject(int selectedRow) {
		long id = leaveIdAt(selectedRow);
		if (id < 0) return;
		int confirm = JOptionPane.showConfirmDialog(null, "Reject this leave application?", "Confirm Rejection", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) return;
		runLeaveAction(() -> LeaveRepository.reject(id));
	}

	public void delete(int selectedRow) {
		long id = leaveIdAt(selectedRow);
		if (id < 0) return;
		int confirm = JOptionPane.showConfirmDialog(null, "Delete this leave application?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) return;
		runLeaveAction(() -> LeaveRepository.delete(id));
	}
}