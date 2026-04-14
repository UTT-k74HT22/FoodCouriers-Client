export const VNPAY_CONFIG = {
  tmnCode: Deno.env.get("VNPAY_TMN_CODE") ?? "",
  hashSecret: Deno.env.get("VNPAY_HASH_SECRET") ?? "",
  paymentUrl: Deno.env.get("VNPAY_PAYMENT_URL") ?? "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
  returnUrl: Deno.env.get("VNPAY_RETURN_URL") ?? "",
  ipnUrl: Deno.env.get("VNPAY_IPN_URL") ?? "",
  appDeepLinkBase: Deno.env.get("APP_PAYMENT_DEEP_LINK_BASE") ?? "com.utt.foodcouriers.client://payment/vnpay/callback",
  locale: "vn",
  currCode: "VND",
  version: "2.1.0",
  command: "pay",
  orderType: "other",
};

function pad2(n: number): string {
  return n < 10 ? `0${n}` : `${n}`;
}

export function formatDateVN(date: Date): string {
  return (
    date.getFullYear().toString() +
    pad2(date.getMonth() + 1) +
    pad2(date.getDate()) +
    pad2(date.getHours()) +
    pad2(date.getMinutes()) +
    pad2(date.getSeconds())
  );
}

export function buildExpireDate(date: Date, minutes = 15): string {
  const d = new Date(date.getTime() + minutes * 60 * 1000);
  return formatDateVN(d);
}

export function sortObject(input: Record<string, string>): Record<string, string> {
  return Object.keys(input)
    .sort()
    .reduce((acc, key) => {
      acc[key] = input[key];
      return acc;
    }, {} as Record<string, string>);
}

export function buildQueryString(params: Record<string, string>): string {
  return Object.entries(params)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value).replace(/%20/g, "+")}`)
    .join("&");
}

function toHex(buffer: ArrayBuffer): string {
  return Array.from(new Uint8Array(buffer))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

export async function signVnpay(params: Record<string, string>): Promise<string> {
  const sorted = sortObject(params);
  const signData = buildQueryString(sorted);
  const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(VNPAY_CONFIG.hashSecret),
    { name: "HMAC", hash: "SHA-512" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(signData));
  return toHex(signature);
}

export async function verifyVnpay(query: URLSearchParams): Promise<boolean> {
  const raw: Record<string, string> = {};
  let secureHash = "";

  for (const [key, value] of query.entries()) {
    if (key === "vnp_SecureHash") {
      secureHash = value;
      continue;
    }
    if (key === "vnp_SecureHashType") continue;
    raw[key] = value;
  }

  const calculated = await signVnpay(raw);
  return calculated.toUpperCase() === secureHash.toUpperCase();
}
