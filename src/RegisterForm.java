import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegisterForm extends JFrame {
    private JTextField username;
    private JButton registerButton;
    private JPanel registerForm;
    private JPasswordField passwordField;
    private final ClientMain client;

    public RegisterForm(ClientMain cilent) {

        this.client = cilent;

        setLocationRelativeTo(null);
        add(registerForm);
        setSize(400, 200);

        registerButton.addActionListener(e -> {
            try {
                client.register(username.getText(), String.valueOf(passwordField.getPassword()));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, e.toString());
                ex.printStackTrace();
            }
        });

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    @Override
    public void dispose() {
        super.dispose();
        client.close();
    }
}
