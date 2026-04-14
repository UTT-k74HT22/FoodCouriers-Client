/**



 */

export const VNPAY_CONFIG = {
  tmnCode: "ANBB2CT1",
  hashSecret: "HV8WLKRENFNZP2ECO14M02RS4HM7E5KN",
  paymentUrl: "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",

  // domain project
  returnUrl: "https://xgpmxfujvjgebtohujgk.supabase.co.functions.supabase.co/vnpay-return",
  ipnUrl: "https://xgpmxfujvjgebtohujgk.supabase.co.functions.supabase.co/vnpay-ipn",

  // deep link app client
  appDeepLinkBase: "com.utt.foodcouriers.client://payment/vnpay/callback",

  locale: "vn",
  currCode: "VND",
  version: "2.1.0",
  command: "pay",
  orderType: "other",
  timezone: "Asia/Ho_Chi_Minh",
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