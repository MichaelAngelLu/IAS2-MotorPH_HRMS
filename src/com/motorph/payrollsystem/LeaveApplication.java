package com.motorph.payrollsystem;

import java.awt.EventQueue;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ToolTipManager;

import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.text.SimpleDateFormat;

import javax.swing.SwingConstants;
import javax.swing.JButton;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;

import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.JComboBox;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextArea;
import java.awt.Insets;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import com.toedter.calendar.JDateChooser;
import javax.swing.border.BevelBorder;

public class LeaveApplication extends JFrame {

	SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd yyyy");
	
	private static final long serialVersionUID = 1L;
	JTable table;
	private JComboBox<Object> comboBoxLeaveType;
	private JDateChooser txtStartDate, txtEndDate;
	private String newLeaveDetails[] = new String[6];
	private List<LeaveRepository.Row> myLeaves = new ArrayList<>();   // row i of the table = myLeaves.get(i)
	private int SlCount, VlCount, ElCount;
	private JTextArea txtLeaveDescription;
	private JButton btnCancel_1;
	JButton btnCancel;
	private LeaveDetails deletedLeave;
	private JLabel lblDescError, lblSL, lblVL, lblEL, lblSLCount, lblVLCount, lblELCount;
	private JTextField txtEmpNo;
	JScrollPane scrollPane;
	private JPanel panelAvailableLeaves;
	LeavePageRedirector redirector;
	/**
	 * Launch the application.
	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					LeaveApplication frame = new LeaveApplication();
//					frame.loadLeaves();
//					frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}
	
	public void applyLeave() {
		txtEmpNo.setText(String.valueOf(SessionManager.employeeNumber()));
		loadLeaves();
		setVisible(true);
	}
	
	

	/**
	 * Create the frame.
	 */
	public LeaveApplication() {
		setTitle("MotorPH Employee Leave");
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
		setSize(600,419);
		setLocationRelativeTo(null);
		getContentPane().setLayout(null);
		
		JLabel lblLeaveApplication = new JLabel("Employee Leave Application");
		lblLeaveApplication.setHorizontalAlignment(SwingConstants.CENTER);
		lblLeaveApplication.setFont(new Font("Tahoma", Font.BOLD, 14));
		lblLeaveApplication.setBounds(192, 24, 225, 17);
		getContentPane().add(lblLeaveApplication);
		
		JButton btnBack = new JButton("Back");
		btnBack.setBounds(496, 338, 64, 23);
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
		
		JPanel payrollInformationPanel_1 = new JPanel();
		payrollInformationPanel_1.setLayout(null);
		payrollInformationPanel_1.setBorder(new LineBorder(new Color(192, 192, 192), 1, true));
		payrollInformationPanel_1.setBounds(10, 51, 564, 143);
		getContentPane().add(payrollInformationPanel_1);
		
		panelAvailableLeaves = new JPanel();
		panelAvailableLeaves.setVisible(false);
		panelAvailableLeaves.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, new Color(128, 128, 128), null));
		panelAvailableLeaves.setBounds(455, 44, 103, 97);
		
		getContentPane().addMouseListener(new MouseAdapter()  {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		        if (panelAvailableLeaves.isVisible() && !panelAvailableLeaves.getBounds().contains(e.getPoint())) {
		        	panelAvailableLeaves.setVisible(false);
		        }
		    }
		});
		
		payrollInformationPanel_1.add(panelAvailableLeaves);
		panelAvailableLeaves.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Leaves Left:");
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setBounds(1, 11, 100, 14);
		panelAvailableLeaves.add(lblNewLabel);
		
		lblSL = new JLabel("SL:");
		lblSL.setBounds(16, 34, 46, 14);
		panelAvailableLeaves.add(lblSL);
		
		lblVL = new JLabel("VL:");
		lblVL.setBounds(16, 52, 46, 14);
		panelAvailableLeaves.add(lblVL);
		
		lblEL = new JLabel("EL:");
		lblEL.setBounds(16, 70, 46, 14);
		panelAvailableLeaves.add(lblEL);
		
		lblSLCount = new JLabel("");
		lblSLCount.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblSLCount.setBounds(67, 34, 46, 14);
		panelAvailableLeaves.add(lblSLCount);
		
		lblVLCount = new JLabel("");
		lblVLCount.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblVLCount.setBounds(67, 52, 46, 14);
		panelAvailableLeaves.add(lblVLCount);
		
		lblELCount = new JLabel("");
		lblELCount.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblELCount.setBounds(67, 70, 46, 14);
		panelAvailableLeaves.add(lblELCount);
		
		JLabel lblAddNewLeave = new JLabel("Add New Leave:");
		lblAddNewLeave.setHorizontalAlignment(SwingConstants.CENTER);
		lblAddNewLeave.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblAddNewLeave.setBounds(185, 6, 193, 14);
		payrollInformationPanel_1.add(lblAddNewLeave);
		
		JLabel lblLeaveType = new JLabel("Leave Type:");
		lblLeaveType.setBounds(235, 28, 102, 14);
		payrollInformationPanel_1.add(lblLeaveType);
		
		JLabel lblStartDate = new JLabel("Start Date:");
		lblStartDate.setBounds(10, 54, 84, 14);
		payrollInformationPanel_1.add(lblStartDate);
		
		comboBoxLeaveType = new JComboBox<Object>(new Object[]{});
		comboBoxLeaveType.setFocusable(false);
		comboBoxLeaveType.setRequestFocusEnabled(false);
		comboBoxLeaveType.setModel(new DefaultComboBoxModel<Object>(new String[] {"Select Leave Type ", "Sick Leave", "Vacation Leave", "Emergency Leave"}));
		comboBoxLeaveType.setBounds(316, 25, 136, 20);
		payrollInformationPanel_1.add(comboBoxLeaveType);
		
		JLabel lblEndDate = new JLabel("End Date:");
		lblEndDate.setBounds(10, 79, 84, 14);
		payrollInformationPanel_1.add(lblEndDate);
		
		JLabel lblReasonForLeave = new JLabel("Reason for ");
		lblReasonForLeave.setBounds(235, 46, 90, 14);
		payrollInformationPanel_1.add(lblReasonForLeave);
		
		txtLeaveDescription = new JTextArea();
		txtLeaveDescription.setMargin(new Insets(4, 4, 4, 4));
		txtLeaveDescription.setBorder(new LineBorder(new Color(128, 128, 128)));
		txtLeaveDescription.setLineWrap(true);
		txtLeaveDescription.setWrapStyleWord(true);
		txtLeaveDescription.setBounds(316, 46, 238, 50);
//		txtLeaveDescription.setToolTipText("A leave descrption can't contain a coma.");
		
		txtLeaveDescription.addKeyListener(new KeyAdapter() {
			@Override
		    public void keyTyped(KeyEvent e) {
		        if (e.getKeyChar() == KeyEvent.VK_COMMA) {
		            e.consume(); // consume the key event to prevent the comma from being entered
		            lblDescError.setVisible(true);
		        }
		    }
		});
		payrollInformationPanel_1.add(txtLeaveDescription);
		
		JButton btnApplyLeave = new JButton("File Leave");
		btnApplyLeave.setMnemonic(KeyEvent.VK_ENTER);
		btnApplyLeave.setBounds(290, 107, 104, 23);
		
		
		payrollInformationPanel_1.add(btnApplyLeave);
		
		btnCancel = new JButton("Cancel");
		btnCancel.setBounds(176, 107, 104, 23);
		btnCancel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				clearFields();
				
			}
		});
		payrollInformationPanel_1.add(btnCancel);
		
		JLabel lblLeave = new JLabel("Leave:");
		lblLeave.setBounds(235, 64, 90, 14);
		payrollInformationPanel_1.add(lblLeave);
		
		txtStartDate = new JDateChooser();
		txtStartDate.setDateFormatString("MMMM dd yyyy");
		txtStartDate.setBounds(89, 50, 136, 20);
		payrollInformationPanel_1.add(txtStartDate);
		
		txtEndDate = new JDateChooser();
		txtEndDate.setDateFormatString("MMMM dd yyyy");
		txtEndDate.setBounds(89, 76, 136, 20);
		payrollInformationPanel_1.add(txtEndDate);
		
		lblDescError = new JLabel("*A leave descrption can't contain a coma.");
		lblDescError.setVisible(false);
		lblDescError.setForeground(new Color(255, 0, 0));
		lblDescError.setFont(new Font("Tahoma", Font.PLAIN, 8));
		lblDescError.setBounds(400, 99, 158, 14);
		payrollInformationPanel_1.add(lblDescError);
		
		JLabel lblEmployeeNo = new JLabel("Employee:");
		lblEmployeeNo.setBounds(10, 28, 102, 14);
		payrollInformationPanel_1.add(lblEmployeeNo);
		
		txtEmpNo = new JTextField();
		txtEmpNo.setBorder(new LineBorder(new Color(128, 128, 128)));
		txtEmpNo.setEnabled(false);
		txtEmpNo.setText(String.valueOf(SessionManager.employeeNumber()));
		txtEmpNo.setEditable(false);
		txtEmpNo.setBounds(89, 25, 136, 20);
		txtEmpNo.setDisabledTextColor(new Color(128, 128, 128));
		txtEmpNo.setBackground(new Color(255,255,255));
		payrollInformationPanel_1.add(txtEmpNo);
		txtEmpNo.setColumns(10);
		
		JButton btnShowCount = new JButton("...");
		btnShowCount.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if (!panelAvailableLeaves.isVisible()) panelAvailableLeaves.setVisible(true);
				else panelAvailableLeaves.setVisible(false);
			}
		});
		btnShowCount.setFocusable(false);
		btnShowCount.setToolTipText("Show Remaining Leaves");
		btnShowCount.setBounds(455, 25, 26, 20);
		payrollInformationPanel_1.add(btnShowCount);
		
		
		scrollPane = new JScrollPane();
		scrollPane.setFont(new Font("Tahoma", Font.BOLD, 11));
		scrollPane.setBounds(10, 217, 564, 107);
		scrollPane.setBorder(null);
		getContentPane().add(scrollPane);
		
		table = new JTable();
		table.setBorder(null);
		table.setDefaultEditor(Object.class, null);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
		    public void valueChanged(ListSelectionEvent event) {
		        int selectedRow = table.getSelectedRow();
		        if (selectedRow != -1) { // If a row is selected
		            String leaveStatus = (String) table.getValueAt(selectedRow, 3);
		            if (leaveStatus.equals("Applied")) {
		                btnCancel_1.setEnabled(true);
		            } else {
		                btnCancel_1.setEnabled(false);
		            }
		        } else {
		        	
		        }
		    }
		});
		
		scrollPane.setViewportView(table);

		JLabel lblFiledLeaves = new JLabel("Filed Leaves:");
		lblFiledLeaves.setHorizontalAlignment(SwingConstants.CENTER);
		lblFiledLeaves.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblFiledLeaves.setBounds(195, 199, 193, 14);
		getContentPane().add(lblFiledLeaves);
		
		btnCancel_1 = new JButton("Cancel");
		btnCancel_1.setEnabled(false);
		btnCancel_1.setBounds(20, 338, 79, 23);
		btnCancel_1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int rowIndex = table.getSelectedRow();
				if (rowIndex < 0 || rowIndex >= myLeaves.size()) {
					return;
				}
				int confirm = JOptionPane.showConfirmDialog(null, "Cancel this leave application?", "Confirm Cancel", JOptionPane.YES_NO_OPTION);
				if (confirm != JOptionPane.YES_OPTION) {
					return;
				}
				try {
					LeaveRepository.cancel(myLeaves.get(rowIndex).id);   // by database id, no CSV rewrite
				} catch (IllegalStateException | SupabaseClient.SupabaseException ex) {
					JOptionPane.showMessageDialog(LeaveApplication.this, ex.getMessage(), "Could not cancel", JOptionPane.ERROR_MESSAGE);
					return;
				}
				redirector = new LeavePageRedirector();
				redirector.redirect();
				setVisible(false);
				dispose();
			}
		});
		getContentPane().add(btnCancel_1);
		
		// Sets icon image for the application
    	ImageIcon icon = new ImageIcon("MOTORPH.png");
    	setIconImage(icon.getImage());
    	setResizable(false);
    	// EVENTS
    	btnApplyLeave.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				checkLeaveDetails();
			}
		});
    	
	}
	
	public void checkLeaveDetails() {
		
		newLeaveDetails[0] = String.valueOf(SessionManager.employeeNumber());
		
		if (comboBoxLeaveType.getSelectedIndex() == 0) {
			JOptionPane.showMessageDialog(LeaveApplication.this, "      Please select leave type.", "Invalid Input", JOptionPane.INFORMATION_MESSAGE, null);
			return;
		} else {
			newLeaveDetails[1] = (String) comboBoxLeaveType.getSelectedItem();
		}
		
		if (comboBoxLeaveType.getSelectedIndex() == 1 && SlCount == 5) {
			JOptionPane.showMessageDialog(LeaveApplication.this, "You have already reached the allowed   \n              maximum sick leaves.", "Leave Maxed Out", JOptionPane.INFORMATION_MESSAGE, null);
			return;
		} else if (comboBoxLeaveType.getSelectedIndex() == 2 && VlCount == 10) {
			JOptionPane.showMessageDialog(LeaveApplication.this, "You have already reached the allowed  \n        maximum vacation leaves.", "Leave Maxed Out", JOptionPane.INFORMATION_MESSAGE, null);
			return;
		} else if (comboBoxLeaveType.getSelectedIndex() == 3 && ElCount == 5) {
			JOptionPane.showMessageDialog(LeaveApplication.this, "You have already reached the allowed  \n        maximum emergency leaves.", "Leave Maxed Out", JOptionPane.INFORMATION_MESSAGE, null);
			return;
		}
		Date selectedStartDate = (Date) txtStartDate.getDate(),
				   selectedEndDate = (Date) txtEndDate.getDate();
			if (selectedStartDate != null && selectedEndDate != null) {
				newLeaveDetails[2] = formatter.format(txtStartDate.getDate());
				newLeaveDetails[3] = formatter.format(txtEndDate.getDate());				
			} else if (selectedStartDate != null) {
				JOptionPane.showMessageDialog(LeaveApplication.this, "Please enter leave end date.", "Date Input Error", JOptionPane.INFORMATION_MESSAGE);
				return;
			} else if (selectedEndDate != null) {
				JOptionPane.showMessageDialog(LeaveApplication.this, "Please enter leave start date.", "Date Input Error", JOptionPane.INFORMATION_MESSAGE);
				return;
			} else {
				JOptionPane.showMessageDialog(LeaveApplication.this, "Please enter leave start and end dates.", "Date Input Error", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			if (!txtLeaveDescription.getText().trim().isEmpty()) {
			    newLeaveDetails[5] = txtLeaveDescription.getText().trim();
			} else {
				JOptionPane.showMessageDialog(LeaveApplication.this, "Leave description should not be blank.", "Please Enter Reason", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			fileLeave();	
	}
	
	public void loadLeaves() {
		// The signed-in user's own leaves, newest first. (An admin's list() also returns everyone's, so filter.)
		String me = String.valueOf(SessionManager.employeeNumber());
		myLeaves = new ArrayList<>();
		try {
			for (LeaveRepository.Row r : LeaveRepository.list()) {
				if (r.details.employeeNumber.equals(me)) {
					myLeaves.add(r);
				}
			}
		} catch (SupabaseClient.SupabaseException ex) {
			JOptionPane.showMessageDialog(this, "Could not load leaves: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}

		// Leaves used per type (cancelled ones don't count)
		SlCount = 0;
		VlCount = 0;
		ElCount = 0;
		for (LeaveRepository.Row r : myLeaves) {
			LeaveDetails d = r.details;
			if (d.leaveStatus.equals("Cancelled")) {
				continue;
			}
			if (d.leaveType.equals("Sick Leave")) {
				SlCount++;
			} else if (d.leaveType.equals("Vacation Leave")) {
				VlCount++;
			} else if (d.leaveType.equals("Emergency Leave")) {
				ElCount++;
			}
		}
		lblSLCount.setText(5 - SlCount + "");
		lblVLCount.setText(10 - VlCount + "");
		lblELCount.setText(5 - ElCount + "");

		// Populate JTable with data (row i of the table = myLeaves.get(i))
		Object[][] data = new Object[myLeaves.size()][5];
		for (int j = 0; j < myLeaves.size(); j++) {
			LeaveDetails d = myLeaves.get(j).details;
			data[j][0] = d.leaveType;
			data[j][1] = d.startDate;
			data[j][2] = d.endDate;
			data[j][3] = d.leaveStatus;
			data[j][4] = d.leaveDescription;
		}
		table.setModel(new DefaultTableModel(
			data,
			new String[] {
				"Leave Type", "Start Date", "End Date", "Leave Status", "Leave Description"
			}
		));
		table.getColumnModel().getColumn(4).setPreferredWidth(84);
		table.repaint();
	}
	public void fileLeave() {
		try {
			LocalDate start = txtStartDate.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate end = txtEndDate.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			LeaveRepository.file(newLeaveDetails[1], start, end, newLeaveDetails[5]);
		} catch (IllegalArgumentException ex) {          // e.g. end date before start date
			JOptionPane.showMessageDialog(LeaveApplication.this, ex.getMessage(), "Invalid Dates", JOptionPane.INFORMATION_MESSAGE);
			return;
		} catch (SupabaseClient.SupabaseException ex) {
			JOptionPane.showMessageDialog(LeaveApplication.this, "Could not file leave: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// Only tell the user it worked after the database accepted it
		JOptionPane.showMessageDialog(LeaveApplication.this, "Leave application successful.", "Success", JOptionPane.INFORMATION_MESSAGE);
		lblDescError.setVisible(false);
		clearFields();
	}
	public void clearFields() {
		comboBoxLeaveType.setSelectedIndex(0);
		txtStartDate.setDate(null);
		txtEndDate.setDate(null);
		txtLeaveDescription.setText(null);
		SlCount = 0;
		VlCount = 0;
		ElCount = 0;
		loadLeaves();
	}
	
}