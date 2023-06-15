use crate::{FromJava, JnixEnv};
use jni::{
    objects::JObject,
    signature::{JavaType, Primitive},
};
use std::net::{IpAddr, Ipv4Addr, Ipv6Addr};

fn read_inet_address_octets<'env, 'o>(env: &JnixEnv<'env>, source: JObject<'o>) -> JObject<'env>
where
    'o: 'env,
{
    let class = env.get_class("java/net/InetAddress");
    let method_id = env
        .get_method_id(&class, "getAddress", "()[B")
        .expect("Failed to get method ID for InetAddress.getAddress()");
    let return_type = JavaType::Array(Box::new(JavaType::Primitive(Primitive::Byte)));

    env.call_method_unchecked(source, method_id, return_type, &[])
        .expect("Failed to call InetAddress.getAddress()")
        .l()
        .expect("Call to InetAddress.getAddress() did not return an object")
}

fn address_from_octets<'env, 'o, A, B>(env: &JnixEnv<'env>, octets: JObject<'o>, mut buffer: B) -> A
where
    'o: 'env,
    A: From<B>,
    B: AsMut<[u8]>,
{
    let mut signed_octets = [0i8; 16];
    let buffer_octets = buffer.as_mut();
    let buffer_size = buffer_octets.len();

    env.get_byte_array_region(octets.into_inner(), 0, &mut signed_octets[..buffer_size])
        .expect("Failed to read octets returned by InetAddress.getAddress()");

    for index in 0..buffer_size {
        buffer_octets[index] = signed_octets[index] as u8;
    }

    A::from(buffer)
}

impl<'env, 'sub_env> FromJava<'env, JObject<'sub_env>> for Ipv4Addr
where
    'env: 'sub_env,
{
    const JNI_SIGNATURE: &'static str = "Ljava/net/Inet4Address;";

    fn from_java(env: &JnixEnv<'env>, source: JObject<'sub_env>) -> Self {
        let octets = read_inet_address_octets(env, source);

        address_from_octets(env, octets, [0u8; 4])
    }
}

impl<'env, 'sub_env> FromJava<'env, JObject<'sub_env>> for Ipv6Addr
where
    'env: 'sub_env,
{
    const JNI_SIGNATURE: &'static str = "Ljava/net/Inet6Address;";

    fn from_java(env: &JnixEnv<'env>, source: JObject<'sub_env>) -> Self {
        let octets = read_inet_address_octets(env, source);

        address_from_octets(env, octets, [0u8; 16])
    }
}

impl<'env, 'sub_env> FromJava<'env, JObject<'sub_env>> for IpAddr
where
    'env: 'sub_env,
{
    const JNI_SIGNATURE: &'static str = "Ljava/net/InetAddress;";

    fn from_java(env: &JnixEnv<'env>, source: JObject<'sub_env>) -> Self {
        let octets = read_inet_address_octets(env, source);
        let octet_count = env
            .get_array_length(octets.into_inner())
            .expect("Failed to get length of byte array returned by InetAddress.getAddress()");

        match octet_count {
            4 => address_from_octets(env, octets, [0u8; 4]),
            16 => address_from_octets(env, octets, [0u8; 16]),
            count => panic!(
                "Invalid number of octets returned by InetAddress.getAddress(): {}",
                count
            ),
        }
    }
}
