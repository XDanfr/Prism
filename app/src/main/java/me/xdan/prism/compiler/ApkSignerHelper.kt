package me.xdan.prism.compiler

import com.android.apksig.ApkSigner
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.util.*

class ApkSignerHelper {

    companion object {
        private val provider = BouncyCastleProvider()

        init {
            Security.removeProvider("BC")
            Security.insertProviderAt(provider, 1)
        }
    }

    fun sign(inputApk: File, outputApk: File, keyPair: KeyPair, certificate: X509Certificate) {
        val signerConfig = ApkSigner.SignerConfig.Builder(
            "CERT",
            keyPair.private,
            listOf(certificate)
        ).build()

        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }

    fun generateKeyPairAndCertificate(): Pair<KeyPair, X509Certificate> {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA", provider)
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val issuer = X500Name("CN=Prism")
        val serial = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date()
        val notAfter = Date(notBefore.time + 365L * 24 * 60 * 60 * 1000 * 30) // 30 years

        val builder = JcaX509v3CertificateBuilder(
            issuer,
            serial,
            notBefore,
            notAfter,
            issuer,
            keyPair.public
        )

        val contentSigner = JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(keyPair.private)
        val certificate = JcaX509CertificateConverter().setProvider(provider).getCertificate(builder.build(contentSigner))

        return Pair(keyPair, certificate)
    }
}
