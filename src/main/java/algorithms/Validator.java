package algorithms;

/**
 * The project's one digit validator.
 *
 * <p>There were three copies of this function — {@code Runner.intOnly}, {@code QueueJava.isInteger}
 * and {@code CircularQueue.isInteger} — and they were not the same function. Measured before this
 * class existed:
 *
 * <pre>
 *   input          intOnly    the two isInteger copies
 *   ""             false      true     the loop never ran
 *   null           false      NPE      no null guard
 *   "2147483648"   false      true     no range check at all
 * </pre>
 *
 * <p>The copies had drifted because nothing said they were related. {@code ArchitectureTest} now
 * forbids {@code Character.isDigit} anywhere but here, so a fourth copy cannot appear under a new
 * name either — the duplication is what made the drift possible in the first place.
 *
 * <p>Which behaviour is canonical is a decision, not an accident. {@code intOnly} was already the
 * strongest of the three, because Task 3 gave it the two guards the queue copies never had, so it is
 * the one that survived. Collapsing the queues onto it is the one behaviour change in Task 15, and
 * it is a fix rather than a regression — see {@link #isInt(String)}.
 *
 * <p>A namespace like the algorithms: final, private constructor, nothing to instantiate. It sits in
 * the default package until Task 18 adds a real one.
 */
public final class Validator {

	private Validator(){
		// Nothing to construct.
	}

	/**
	 * Whether {@code text} is a non-negative {@code int} written out in full.
	 *
	 * <p>Three rules, all of which some caller needed at some point:
	 * <ul>
	 *   <li><b>Not null.</b> Every prompt in this app can hand back a null when the user presses
	 *       Cancel, and the validator is the first thing that sees it. Throwing there would turn a
	 *       cancel into a crash — which is exactly what the two queue copies did.
	 *   <li><b>Not empty, and digits only.</b> An empty field is not a number. Note this rejects
	 *       {@code "-1"} and {@code "+1"} as well: every value this app asks for is a count or a
	 *       length, and neither is meaningfully negative.
	 *   <li><b>Must fit in an int.</b> This is the check the queue copies lacked, and their absence
	 *       was a live crash: both demos read a size with {@code Scanner.next()}, accepted any run of
	 *       digits, and passed it to {@code Integer.parseInt}, so typing {@code 99999999999} ended
	 *       the program with {@code NumberFormatException}. Every caller in this project hands the
	 *       result straight to {@code parseInt}, so refusing here is what stops the throw.
	 * </ul>
	 *
	 * <p>Leading zeros are accepted however long the run: {@code parseInt} reads
	 * {@code "0000000000000000000005"} as 5, so refusing it for length would be refusing a number
	 * that fits.
	 *
	 * @param text text straight from a prompt; may be null
	 * @return true only for a bare run of digits denoting a value an {@code int} can hold
	 */
	public static boolean isInt(String text){
		if(text == null || text.isEmpty()){
			return false;
		}
		for(int i = 0; i < text.length(); i++){
			if(!Character.isDigit(text.charAt(i))){
				return false;
			}
		}
		try{
			Integer.parseInt(text);
			return true;
		}catch(NumberFormatException e){
			// More digits than an int can hold. The loop above already proved they are all
			// digits, so this is reached only by magnitude.
			return false;
		}
	}

	/**
	 * Whether {@code text} is one or more letters and nothing else.
	 *
	 * <p>The check behind both queue demos' {@code y/n} question. Two identical copies of it existed
	 * — one private method per queue class — so this is the third piece of duplication this class
	 * absorbed. It is not named in the Task 15 brief, which listed only the digit validator, but the
	 * brief's own wording is "the triplicated validators" and half the duplication would otherwise
	 * have been left standing.
	 *
	 * <p>Unlike {@link #isInt(String)}, collapsing these changed nothing for the caller: both copies
	 * were identical to each other. They did throw on null, which this does not; that path is
	 * unreachable today because the callers pass {@code Scanner.next()}, which never returns null.
	 *
	 * <p><b>An empty string is accepted, and that is deliberate.</b> Both 2019 copies returned true
	 * for {@code ""} because their loop never ran, so preserving it is behaviour-preserving. It looks
	 * like an oversight — an empty answer is not a yes or a no — and it is recorded as existing
	 * behaviour in {@code ValidatorTest} so that fixing it later is a decision rather than a
	 * drive-by.
	 *
	 * @param text text straight from a prompt; may be null
	 * @return true for a non-empty run of letters, and also for the empty string
	 */
	public static boolean isLetters(String text){
		if(text == null){
			return false;
		}
		for(int i = 0; i < text.length(); i++){
			if(!Character.isLetter(text.charAt(i))){
				return false;
			}
		}
		return true;
	}
}
