use crate::{AsJValue, IntoJava, JnixEnv};
use jni::{
    objects::{AutoLocal, JObject, JValue},
    signature::JavaType,
    sys::jint,
};
use std::net::{IpAddr, Ipv4Addr, Ipv6Addr, SocketAddr};

fn ipvx_addr_into_java<'borrow, 'env: 'borrow>(
    original_octets: &[u8],
    env: &'borrow JnixEnv<'env>,
) -> AutoLocal<'env, 'borrow> {
    let class = env.get_class("java/net/InetAddress");
    let constructor = env
        .get_static_method_id(&class, "getByAddress", "([B)Ljava/net/InetAddress;")
        .expect("Failed to get InetAddress.getByAddress method ID");

    let octets_array = env
        .new_byte_array(original_octets.len() as i32)
        .expect("Failed to create byte array to store IP address");

    let octet_data: Vec<i8> = original_octets
        .into_iter()
        .map(|octet| *octet as i8)
        .collect();

    env.set_byte_array_region(octets_array, 0, &octet_data)
        .expect("Failed to copy IP address octets to byte array");

    let octets = env.auto_local(JObject::from(octets_array));
    let result = env
        .call_static_method_unchecked(
            &class,
            constructor,
            JavaType::Object("java/net/InetAddress".to_owned()),
            &[octets.as_jvalue()],
        )
        .expect("Failed to create InetAddress Java object");

    match result {
        JValue::Object(object) => env.auto_local(object),
        value => {
            panic!(
                "InetAddress.getByAddress returned an invalid value: {:?}",
                value
            );
        }
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for Ipv4Addr {
    const JNI_SIGNATURE: &'static str = "Ljava/net/InetAddress;";

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        ipvx_addr_into_java(self.octets().as_ref(), env)
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for Ipv6Addr {
    const JNI_SIGNATURE: &'static str = "Ljava/net/InetAddress;";

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        ipvx_addr_into_java(self.octets().as_ref(), env)
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for IpAddr {
    const JNI_SIGNATURE: &'static str = "Ljava/net/InetAddress;";

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        match self {
            IpAddr::V4(address) => address.into_java(env),
            IpAddr::V6(address) => address.into_java(env),
        }
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for SocketAddr {
    const JNI_SIGNATURE: &'static str = "Ljava/net/InetSocketAddress;";

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        let ip_address = self.ip().into_java(env);
        let port = self.port() as jint;
        let parameters = [JValue::Object(ip_address.as_obj()), JValue::Int(port)];

        let class = env.get_class("java/net/InetSocketAddress");
        let object = env
            .new_object(&class, "(Ljava/net/InetAddress;I)V", &parameters)
            .expect("Failed to convert SocketAddr Rust type into InetSocketAddress Java object");

        env.auto_local(object)
    }
}
