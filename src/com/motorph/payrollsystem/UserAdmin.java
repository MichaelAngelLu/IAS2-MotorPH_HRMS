package com.motorph.payrollsystem;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.awt.event.ActionEvent;
import javax.swing.DefaultComboBoxModel;

public class UserAdmin extends JFrame {

	private JTextField txtTemPass;
	JComboBox comboBoxAvailableEmpNo, comboBoxActiveAccounts, comboBoxSystemRole;
	/**
	 * Launch the application.
	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					UserAdmin frame = new UserAdmin();
//					frame.findAvailableEmployeeNumbers();
//					frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}
	
	public void displayUserAdmin() {
		findAvailableEmployeeNumbers();
		findActiveAccounts();
		setVisible(true);
	}

	private void showError(String message) {
		JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/** Employees that exist but do not have a login yet. */
	public void findAvailableEmployeeNumbers() {
		comboBoxAvailableEmpNo.removeAllItems();
		try {
			Set<Integer> haveAccount = new HashSet<>();
			for (AdminAccountService.Account a : AdminAccountService.listAccounts()) {
				haveAccount.add(a.employeeNumber);
			}
			for (EmployeeDetails e : EmployeeRepository.findAll()) {
				if (!haveAccount.contains(Integer.parseInt(e.employeeNumber))) {
					comboBoxAvailableEmpNo.addItem(e.employeeNumber);
				}
			}
		} catch (SecurityException | SupabaseClient.SupabaseException ex) {
			showError("Could not load employees: " + ex.getMessage());
		}
	}

	/** Existing accounts that can be deleted (not the protected first employee, and not yourself). */
	public void findActiveAccounts() {
		comboBoxActiveAccounts.removeAllItems();
		try {
			for (AdminAccountService.Account a : AdminAccountService.listAccounts()) {
				if (a.employeeNumber == 10001 || a.employeeNumber == SessionManager.employeeNumber()) {
					continue;
				}
				comboBoxActiveAccounts.addItem(String.valueOf(a.employeeNumber));
			}
		} catch (SecurityException | SupabaseClient.SupabaseException ex) {
			showError("Could not load accounts: " + ex.getMessage());
		}
	}

	/** Returns true if the account was created. */
	public boolean createNewAccount() {
		Object selected = comboBoxAvailableEmpNo.getSelectedItem();
		if (selected == null) {
			JOptionPane.showMessageDialog(null, "Every employee already has an account.", "Input Error", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		int newEmp = Integer.parseInt((String) selected);
		String newPass = txtTemPass.getText();

		if (newPass.equals("")) {
			JOptionPane.showMessageDialog(null, "        Please enter password.", "Input Error", JOptionPane.WARNING_MESSAGE);
			return false;
		} else if (newPass.length() < 8) {
			JOptionPane.showMessageDialog(null, "Password should contain atleast 8 characters.", "Input Error", JOptionPane.WARNING_MESSAGE);
			return false;
		}

		if (comboBoxSystemRole.getSelectedIndex() == 0) {
			JOptionPane.showMessageDialog(null, "Please enter new account system role.", "Input Error", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		String role = "Admin".equals(comboBoxSystemRole.getSelectedItem()) ? "admin" : "employee";

		try {
			AdminAccountService.create(newEmp, newPass, role);
		} catch (SecurityException | IllegalArgumentException | SupabaseClient.SupabaseException ex) {
			JOptionPane.showMessageDialog(null, ex.getMessage(), "Could not create account", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		JOptionPane.showMessageDialog(null,
			"Account for employee " + newEmp + " created.\nThey will be asked to change the temporary password.",
			"Create New Successful", JOptionPane.INFORMATION_MESSAGE);
		return true;
	}

	public void deleteAccount() {
		Object selected = comboBoxActiveAccounts.getSelectedItem();
		if (selected == null) {
			return;
		}
		int delEmp = Integer.parseInt((String) selected);

		int confirm = JOptionPane.showConfirmDialog(null, "Delete " + delEmp + "'s Employee Account?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) return;

		try {
			AdminAccountService.delete(delEmp);
		} catch (SecurityException | SupabaseClient.SupabaseException ex) {
			JOptionPane.showMessageDialog(null, ex.getMessage(), "Could not delete account", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JOptionPane.showMessageDialog(null, "Account for employee " + delEmp + " deleted.", "Delete Successful", JOptionPane.INFORMATION_MESSAGE);
	}
	/**
	 * Create the frame.
	 */
	public UserAdmin() {
		setTitle("Payroll Account Administration");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(527, 311);
		setLocationRelativeTo(null);
		getContentPane().setLayout(null);
		// Sets icon image for the application
    	ImageIcon icon = new ImageIcon("MOTORPH.png");
    	setIconImage(icon.getImage());
		
		JLabel lblUserAdministration = new JLabel("User Administration");
		lblUserAdministration.setHorizontalAlignment(SwingConstants.CENTER);
		lblUserAdministration.setFont(new Font("Tahoma", Font.BOLD, 14));
		lblUserAdministration.setBounds(143, 23, 225, 17);
		getContentPane().add(lblUserAdministration);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
		tabbedPane.setBounds(10, 51, 491, 171);
		getContentPane().add(tabbedPane);
		
		JPanel panel = new JPanel();
		tabbedPane.addTab("Create New", null, panel, null);
		panel.setLayout(null);
		
		JLabel lblSelectAvailableEmp = new JLabel("Select Available Emp #:");
		lblSelectAvailableEmp.setBounds(10, 26, 161, 14);
		panel.add(lblSelectAvailableEmp);
		
		comboBoxAvailableEmpNo = new JComboBox(new Object[]{});
		comboBoxAvailableEmpNo.setBounds(188, 22, 197, 22);
		panel.add(comboBoxAvailableEmpNo);
		
		JLabel lblEnterTemporaryPassword = new JLabel("Enter Temporary Password:");
		lblEnterTemporaryPassword.setBounds(10, 56, 161, 14);
		panel.add(lblEnterTemporaryPassword);
		
		txtTemPass = new JTextField();
		txtTemPass.setBounds(188, 53, 197, 20);
		panel.add(txtTemPass);
		txtTemPass.setColumns(10);
		
		JButton btnCreate = new JButton("Create");
		btnCreate.setBounds(166, 132, 80, 23);
		btnCreate.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (createNewAccount()) {
					txtTemPass.setText("");
					comboBoxSystemRole.setSelectedIndex(0);
				}
				findAvailableEmployeeNumbers();
				findActiveAccounts();
			}
		});
		panel.add(btnCreate);
		
		JLabel lblEnterSystemRole = new JLabel("Enter System Role:");
		lblEnterSystemRole.setBounds(10, 86, 161, 14);
		panel.add(lblEnterSystemRole);
		
		comboBoxSystemRole = new JComboBox(new Object[]{});
		comboBoxSystemRole.setModel(new DefaultComboBoxModel(new String[] {"---", "Admin", "User"}));
		comboBoxSystemRole.setBounds(188, 82, 197, 22);
		panel.add(comboBoxSystemRole);
		
		JPanel panel_1 = new JPanel();
		tabbedPane.addTab("Delete", null, panel_1, null);
		panel_1.setLayout(null);
		
		JLabel lblSelectAccountTo = new JLabel("Select Account to Delete:");
		lblSelectAccountTo.setBounds(10, 26, 161, 14);
		panel_1.add(lblSelectAccountTo);
		
		comboBoxActiveAccounts = new JComboBox(new Object[]{});
		comboBoxActiveAccounts.setBounds(188, 22, 197, 22);
		panel_1.add(comboBoxActiveAccounts);
		
		JButton btnDelete = new JButton("Delete");
		btnDelete.setBounds(166, 132, 80, 23);
		btnDelete.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteAccount();
				findAvailableEmployeeNumbers();
				findActiveAccounts();
			}
		});
		panel_1.add(btnDelete);
	}
}