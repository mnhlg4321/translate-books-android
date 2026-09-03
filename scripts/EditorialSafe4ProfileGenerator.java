import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfile;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfileValidationResult;
import com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfileValidator;
import com.ml.tblandroidtxt.editorial.pack.EditorialSafe4TrustedProfileFactory;

/** Small build-time bridge; it is not part of the Android runtime. */
public final class EditorialSafe4ProfileGenerator {
    private EditorialSafe4ProfileGenerator() { }

    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Usage: <sourceCommit> <createdAt>");
        EditorialEngineContractProfile profile = EditorialSafe4TrustedProfileFactory.create(args[0], args[1]);
        EditorialEngineContractProfileValidationResult validation =
                new EditorialEngineContractProfileValidator().validate(profile);
        if (!validation.isValid()) throw new IllegalStateException("Generated profile is invalid: " + validation.issues());
        System.out.print(com.ml.tblandroidtxt.editorial.pack.EditorialEngineContractProfileCanonicalizer.canonicalJson(profile));
    }
}
