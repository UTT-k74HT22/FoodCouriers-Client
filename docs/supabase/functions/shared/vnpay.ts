/**



 */

export const VNPAY_CONFIG = {
  tmnCode: "K1GG6ZU3",
  hashSecret: "I845ZAAM8ZD9CI7SJPX38NGUX0OGHJZC",
  paymentUrl: "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",

  // domain project
  returnUrl: "https://xgpmxfujvjgebtohujgk.supabase.co/functions/v1/vnpay-return",
  ipnUrl: "https://xgpmxfujvjgebtohujgk.supabase.co/functions/v1/vnpay-ipn",

  // deep link app client
  appDeepLinkBase: "com.utt.foodcouriers.client://payment/vnpay/callback",

  locale: "vn",
  currCode: "VND",
  version: "2.1.0",
  command: "pay",
  orderType: "other",
};

function pad2(n: number): string {
  return n < 10 ? `0${n}` : `${n}`;
}

function toVietnamTime(date: Date): Date {
  const utcTime = date.getTime() + date.getTimezoneOffset() * 60 * 1000;
  const vietnamOffsetMs = 7 * 60 * 60 * 1000;
  return new Date(utcTime + vietnamOffsetMs);
}

export function formatDateVN(date: Date): string {
  const vnDate = toVietnamTime(date);
  return (
    vnDate.getFullYear().toString() +
    pad2(vnDate.getMonth() + 1) +
    pad2(vnDate.getDate()) +
    pad2(vnDate.getHours()) +
    pad2(vnDate.getMinutes()) +
    pad2(vnDate.getSeconds())
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

export function signVnpay(params: Record<string, string>): string {
  const sorted = sortObject(params);
  const signData = buildQueryString(sorted);
  return createHmac("sha512", VNPAY_CONFIG.hashSecret)
    .update(Buffer.from(signData, "utf-8"))
    .digest("hex");
}

export function verifyVnpay(query: URLSearchParams): boolean {
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

  const calculated = signVnpay(raw);
  return calculated.toUpperCase() === secureHash.toUpperCase();
}
