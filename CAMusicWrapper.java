import java.awt.Frame;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;

public class CAMusicWrapper extends Frame {

	CAMusic cam;

	public CAMusicWrapper() {
		cam = new CAMusic();

		constructComponents();
	}

	public void constructComponents() {
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				System.exit(0);
			}
		});

		setLayout(new BorderLayout());

		setTitle("CAMusic");

		cam = new CAMusic();
		cam.init();
		cam.setup();

		setMinimumSize(new Dimension(
				cam.getWidth(),
				cam.getHeight())
		);
		setPreferredSize(new Dimension(
				cam.getWidth(),
				cam.getHeight())
		);
		
		add(cam, BorderLayout.CENTER);
	}

	public static void main(String[] args) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				CAMusicWrapper camw = new CAMusicWrapper();
				camw.setVisible(true);
			}
		});
	}
}		
