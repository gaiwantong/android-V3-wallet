package info.blockchain.wallet.send;

import android.content.Context;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;

import org.junit.Test;
import org.mockito.Mock;

import java.math.BigInteger;

/**
 * Created by riaanvos on 08/08/16.
 */
public class SendFactoryTest {

    @Mock
    Context mMockContext;

    @Test
    public void testPrepareSend() throws Exception {

        MultiAddrFactory f = MultiAddrFactory.getInstance();
        f.refreshXPUBData(new String[]{"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG"});
        Thread.sleep(3000);
        SendFactory sf = SendFactory.getInstance(mMockContext);

        sf.prepareSend("xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG",
                BigInteger.valueOf(10000),
                BigInteger.ZERO,
                "\n" +
                        "\n" +
                        "{\n" +
                        "    \n" +
                        "    \"unspent_outputs\":[\n" +
                        "    \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"af14ce9ad0a92b67b4f0ed8a77cfc5629ed27c6ef0854c4e488cf3f0fe681663\",\n" +
                        "            \"tx_hash_big_endian\":\"631668fef0f38c484e4c85f06e7cd29e62c5cf778aedf0b4672ba9d09ace14af\",\n" +
                        "            \"tx_index\":159186095,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a914bd5c6ac9ef46b7fbca2df5b9aa29aa58f2f3913188ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/1\"\n" +
                        "            },\n" +
                        "            \"value\": 31037,\n" +
                        "            \"value_hex\": \"793d\",\n" +
                        "            \"confirmations\":4995\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"3c959af66cfff6595e8842f177196409a0430552f9a72843ad9f4d573f50039c\",\n" +
                        "            \"tx_hash_big_endian\":\"9c03503f574d9fad4328a7f9520543a009641977f142885e59f6ff6cf69a953c\",\n" +
                        "            \"tx_index\":159186185,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a914412d1218eda7054f49f4810cff251ca6d74f050e88ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/2\"\n" +
                        "            },\n" +
                        "            \"value\": 100180,\n" +
                        "            \"value_hex\": \"018754\",\n" +
                        "            \"confirmations\":4995\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"9abc32beb4113ea284f35de6a6b46dd8caf25171a2ec95af8e8d841380a74983\",\n" +
                        "            \"tx_hash_big_endian\":\"8349a78013848d8eaf95eca27151f2cad86db4a6e65df384a23e11b4be32bc9a\",\n" +
                        "            \"tx_index\":159186693,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a9143ea04e8dc363a9349c12e73054715be4f526c41588ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/3\"\n" +
                        "            },\n" +
                        "            \"value\": 162031,\n" +
                        "            \"value_hex\": \"0278ef\",\n" +
                        "            \"confirmations\":4981\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"a639c4b89eb832cb4c4726e40de91c91394284a32fd74f3f05492d651d1f5790\",\n" +
                        "            \"tx_hash_big_endian\":\"90571f1d652d49053f4fd72fa3844239911ce90de426474ccb32b89eb8c439a6\",\n" +
                        "            \"tx_index\":159815749,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a9145a410d171af1b4a758c930a5562055552a76eed388ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/6\"\n" +
                        "            },\n" +
                        "            \"value\": 81479,\n" +
                        "            \"value_hex\": \"013e47\",\n" +
                        "            \"confirmations\":4584\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"55da433c41fc09d9f6e927fd347ecdad443b84deed86dfc1f74d77a4b716f7d7\",\n" +
                        "            \"tx_hash_big_endian\":\"d7f716b7a4774df7c1df86edde843b44adcd7e34fd27e9f6d909fc413c43da55\",\n" +
                        "            \"tx_index\":159815704,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a914c2301b954542b63b1c24d71b7b113d1301cce97a88ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/5\"\n" +
                        "            },\n" +
                        "            \"value\": 50000,\n" +
                        "            \"value_hex\": \"00c350\",\n" +
                        "            \"confirmations\":4584\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"4fdaa4f58cf6f60148d93b056d44de7d41518686418df73f66541e401cc039d0\",\n" +
                        "            \"tx_hash_big_endian\":\"d039c01c401e54663ff78d41868651417dde446d053bd94801f6f68cf5a4da4f\",\n" +
                        "            \"tx_index\":160644137,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a9144153de9e11787184e5a1c78f55f24796b6b9487688ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/7\"\n" +
                        "            },\n" +
                        "            \"value\": 71149,\n" +
                        "            \"value_hex\": \"0115ed\",\n" +
                        "            \"confirmations\":3961\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"3b344062cf31465ef355c8fc59f93397eed7c26600823f4f0a5032cba2b2e18c\",\n" +
                        "            \"tx_hash_big_endian\":\"8ce1b2a2cb32500a4f3f820066c2d7ee9733f959fcc855f35e4631cf6240343b\",\n" +
                        "            \"tx_index\":161055450,\n" +
                        "            \"tx_output_n\": 1,\n" +
                        "            \"script\":\"76a914770c57e2fc31fc437b316a541f7e59300c4c8ddd88ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/1/7\"\n" +
                        "            },\n" +
                        "            \"value\": 1585470,\n" +
                        "            \"value_hex\": \"18313e\",\n" +
                        "            \"confirmations\":3701\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"2a34f4ba5f47aef87b29aef190fde7b6a9f966d67b486eb076c8669f49ae86c0\",\n" +
                        "            \"tx_hash_big_endian\":\"c086ae499f66c876b06e487bd666f9a9b6e7fd90f1ae297bf8ae475fbaf4342a\",\n" +
                        "            \"tx_index\":161063320,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a914c41b0b43e5ee0efb1db4c5201c0e8af48e67aa7a88ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/8\"\n" +
                        "            },\n" +
                        "            \"value\": 10000,\n" +
                        "            \"value_hex\": \"2710\",\n" +
                        "            \"confirmations\":3696\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"9218bf142cb3dd1a43c1ef56ebda75724b0c71e1adcf1deea0c3a69c56cf1fa4\",\n" +
                        "            \"tx_hash_big_endian\":\"a41fcf569ca6c3a0ee1dcfade1710c4b7275daeb56efc1431addb32c14bf1892\",\n" +
                        "            \"tx_index\":164259119,\n" +
                        "            \"tx_output_n\": 1,\n" +
                        "            \"script\":\"76a914a8410ba72706a36fb6d841e228a581c944ed808688ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/9\"\n" +
                        "            },\n" +
                        "            \"value\": 10000,\n" +
                        "            \"value_hex\": \"2710\",\n" +
                        "            \"confirmations\":1678\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"3f40019a82b8ee50bb39a18fdcf726cb39c5cdb2be461fb6970912e0265e5e22\",\n" +
                        "            \"tx_hash_big_endian\":\"225e5e26e0120997b61f46beb2cdc539cb26f7dc8fa139bb50eeb8829a01403f\",\n" +
                        "            \"tx_index\":164259487,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a914d1ca2dbb7f7739e466c66447b874c90ddff824e788ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/11\"\n" +
                        "            },\n" +
                        "            \"value\": 10000,\n" +
                        "            \"value_hex\": \"2710\",\n" +
                        "            \"confirmations\":1678\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"9bfe4885f8c24f3ac6c0434ea317ed89d77e8a0283b2764b8f65debe22b54ffb\",\n" +
                        "            \"tx_hash_big_endian\":\"fb4fb522bede658f4b76b283028a7ed789ed17a34e43c0c63a4fc2f88548fe9b\",\n" +
                        "            \"tx_index\":164259248,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a914b9340ccfbeb666df67bc0c8938c9034ceaed424788ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/10\"\n" +
                        "            },\n" +
                        "            \"value\": 40000,\n" +
                        "            \"value_hex\": \"009c40\",\n" +
                        "            \"confirmations\":1678\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"57c9793be1cf347c57bf8f7804a0f866c1b9c25bffb33c529a98399b2be3e716\",\n" +
                        "            \"tx_hash_big_endian\":\"16e7e32b9b39989a523cb3ff5bc2b9c166f8a004788fbf577c34cfe13b79c957\",\n" +
                        "            \"tx_index\":164268620,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a914f7589a594d744ce141f5261ef0c1635fdb0dfad188ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/12\"\n" +
                        "            },\n" +
                        "            \"value\": 18290,\n" +
                        "            \"value_hex\": \"4772\",\n" +
                        "            \"confirmations\":1674\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"f6d44e430336e0839076bc805f8f823079406c1ef718be9fc557f06fee16594e\",\n" +
                        "            \"tx_hash_big_endian\":\"4e5916ee6ff057c59fbe18f71e6c407930828f5f80bc769083e03603434ed4f6\",\n" +
                        "            \"tx_index\":164269333,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a914c3fc913e894345fa8b0d6af1cbd64a162dc58ac188ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/13\"\n" +
                        "            },\n" +
                        "            \"value\": 67000,\n" +
                        "            \"value_hex\": \"0105b8\",\n" +
                        "            \"confirmations\":1673\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"21ba995fc6d8afe2673e811a5301c0736cab9bc1f361027932f71cb4abc755d0\",\n" +
                        "            \"tx_hash_big_endian\":\"d055c7abb41cf732790261f3c19bab6c73c001531a813e67e2afd8c65f99ba21\",\n" +
                        "            \"tx_index\":164325496,\n" +
                        "            \"tx_output_n\": 0,\n" +
                        "            \"script\":\"76a914995eea77c16d09bac73f67a7a00d9f49eb892fae88ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/0/14\"\n" +
                        "            },\n" +
                        "            \"value\": 21290,\n" +
                        "            \"value_hex\": \"532a\",\n" +
                        "            \"confirmations\":1638\n" +
                        "        },\n" +
                        "      \n" +
                        "        {\n" +
                        "            \"tx_hash\":\"c8fc3753b400e687af80cef5b7de6df4c7888ec48c4b08050f6c5cf558013a5a\",\n" +
                        "            \"tx_hash_big_endian\":\"5a3a0158f55c6c0f05084b8cc48e88c7f46ddeb7f5ce80af87e600b45337fcc8\",\n" +
                        "            \"tx_index\":165189252,\n" +
                        "            \"tx_output_n\": 1,\n" +
                        "            \"script\":\"76a9145c29e9598f0bb040c9c9da33850ba50f633c706888ac\",\n" +
                        "            \"xpub\" : {\n" +
                        "                \"m\" : \"xpub6DB8Ny4BBUA5z4E8Mhc5zYJnxD9SvhqchpfsqzngNXkeMtUWSBJj3K8wS3NVjpMno6n2g2xoj9NMwpsKBLfX82BtTAYYK7xqh2uXgdoD9qG\",\n" +
                        "                \"path\" : \"M/1/28\"\n" +
                        "            },\n" +
                        "            \"value\": 2250419,\n" +
                        "            \"value_hex\": \"2256b3\",\n" +
                        "            \"confirmations\":1074\n" +
                        "        }\n" +
                        "      \n" +
                        "    ]\n" +
                        "}");
    }
}