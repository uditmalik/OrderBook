import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class BidMapComparator implements Comparator<Double>{
	// Comparator class for bid_map
	@Override
	public int compare(Double d1, Double d2) {
		return Double.compare(d2, d1);
	}
}
class Order { // Class representing an Order from the input
	private String timestamp;
	private String type;
	private String id;
	private String side;
	private String price;
	private String size;

	public Order(String timestamp, String type, String id, String side, String price, String size) {
		this.timestamp=timestamp;
		this.type=type;
		this.id=id;
		this.side=side;
		this.price=price;
		this.size=size;
	}
	public Order(String timestamp, String type, String id, String size) {
		this.timestamp=timestamp;
		this.type=type;
		this.id=id;
		this.size=size;
	}
	public long getTimestamp(){
		return Long.parseLong(timestamp);
	}
	public String getType() {
		return type;
	}
	public String getOrderid() {
		return id;
	}
	public char getSide() {
		return side.charAt(0);
	}
	public double getPrice() {
		return Double.parseDouble(price);
	}
	public int getSize() {
		return Integer.parseInt(size);
	}

}
class OrderBook {
	private int target_size;
	// Variables to keep count of total number of asks and bids
	private int ask_total=0; 
	private int bid_total=0;
	// bid_map stores <Price,Count> for all bids sorted by Key in Descending order
	// ask_map stores <Price,Count> for all asks sorted by Key in Ascending order
	private TreeMap<Double, Integer> bid_map=new TreeMap<Double, Integer>(new BidMapComparator());
	private TreeMap<Double, Integer> ask_map=new TreeMap<Double, Integer>();

	public OrderBook(int t_size) {
		this.target_size=t_size;
	}

	private class Pair { 
		private char s;
		private double price;
		public Pair(char s, double pr) {
			this.s=s;
			this.price=pr;
		}
	}
	// order_map is a HashMap storing order string as key and price and side as value. 
	private  Map<String,Pair> order_map=new HashMap<String,Pair>(); 
	
	private void add_order(Order o2) {
		Pair obj=new Pair(o2.getSide(),o2.getPrice());
		order_map.put(o2.getOrderid(),obj);
		char side=o2.getSide();

		switch(side) {
		case 'B': // Adding bid order
			
			if(bid_map.containsKey(o2.getPrice())) {
				bid_map.put(o2.getPrice(), bid_map.get(o2.getPrice())+o2.getSize());
			}
			else {
				bid_map.put(o2.getPrice(), o2.getSize());
			}
			bid_total+=o2.getSize();
			if(bid_total>=target_size) {
				double income=calculate(target_size,bid_map);
				display(o2.getTimestamp(),"S",income);
			}
			break;
		case 'S': // Adding a Sell order
			if(ask_map.containsKey(o2.getPrice())) {
				ask_map.put(o2.getPrice(), ask_map.get(o2.getPrice())+o2.getSize());
			}
			else {
				ask_map.put(o2.getPrice(), o2.getSize());
			}
			ask_total+=o2.getSize();
			if(ask_total>=target_size) {
				double expense=calculate(target_size,ask_map);
				display(o2.getTimestamp(),"B",expense);
			}
			break;
		}
	}
	private void remove_order(Order o1) {
		if(order_map.containsKey(o1.getOrderid())) {
			Pair obj=order_map.get(o1.getOrderid());
			char side=obj.s;

			switch(side) {
			case 'B': // Removing a Bid order 
				bid_map.put(obj.price, bid_map.get(obj.price)-o1.getSize());

				if(bid_total>=target_size) {
					bid_total-=o1.getSize();
					if(bid_total<target_size)
						display(o1.getTimestamp(),"S",0);
					else {
						double income=calculate(target_size,bid_map);
						display(o1.getTimestamp(),"S",income);
					}

				}
				else {
					bid_total-=o1.getSize();
				}
				break;	


			case 'S': // Removing a Sell order
				ask_map.put(obj.price, ask_map.get(obj.price)-o1.getSize());
				if(ask_total>=target_size) {
					ask_total-=o1.getSize();
					if(ask_total<target_size)
						display(o1.getTimestamp(),"B",0);
					else {
						double expense=calculate(target_size,ask_map);
						display(o1.getTimestamp(),"B",expense);
					}

				}
				else {
					ask_total-=o1.getSize();
				}
				break;
			}
		}
		else {
			System.err.println("Invalid entry to remove order");
		}
	}

	private double calculate(int t_size,TreeMap<Double,Integer> tm) {
		// Calculating the incurred expense or income 
		double value=0;
		for(Map.Entry<Double, Integer> entry: tm.entrySet()) {
			int size=Math.min(entry.getValue(),t_size);
			value+=entry.getKey()*size;
			t_size-=size;
			if(t_size==0) 
				break;

		}
		return value;
	} 
	private void display(long tstamp, String type, double val) {
		if(val==0){
			System.out.println(tstamp+" "+type+" "+"NA");
		}
		else
			System.out.println(tstamp+" "+type+" "+val);
	}
	public void process_feed(String line) {
		// Function to parse the incoming feed
		try {
		String[] parts=line.split(" ");
		String s=parts[1];
		
			switch(s) {
			case "R":
				Order o1=new Order(parts[0],parts[1],parts[2],parts[3]);
				remove_order(o1);
				break;
			case "A":
				Order o2=new Order(parts[0],parts[1],parts[2],parts[3],parts[4],parts[5]);
				add_order(o2);
				break;
			default:
				System.err.println("Could not process this line >> "+line);
			}
		}
		catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}
}
public class Pricer {
	public static void main(String[] args) {
		if(args.length!=1)
			System.err.println("This program considers only 1 argument");
		final int target_size=Integer.parseInt(args[0]);
		OrderBook ob=new OrderBook(target_size);
		try {
			BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
			String line=null;
			while((line=br.readLine())!=null) {
				if(line.length()==0)
					break;
				ob.process_feed(line);
			}
			br.close();
		}
		catch(IOException io) {
			io.printStackTrace();
		}
		System.out.println("Exiting the program");
	}
}
