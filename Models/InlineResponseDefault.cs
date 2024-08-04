using System.Text;
using System.Runtime.Serialization;
using Newtonsoft.Json;

namespace Congratulator.Models
{
    [DataContract]
    public partial class InlineResponseDefault : IEquatable<InlineResponseDefault>
    { 
        [DataMember(Name="message")]
        public string Message { get; set; }

        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append("class InlineResponseDefault {\n");
            sb.Append("  Message: ").Append(Message).Append("\n");
            sb.Append("}\n");
            return sb.ToString();
        }

        public string ToJson()
        {
            return JsonConvert.SerializeObject(this, Formatting.Indented);
        }

        public override bool Equals(object obj)
        {
            if (ReferenceEquals(null, obj)) return false;
            if (ReferenceEquals(this, obj)) return true;
            return obj.GetType() == GetType() && Equals((InlineResponseDefault)obj);
        }

        public bool Equals(InlineResponseDefault other)
        {
            if (ReferenceEquals(null, other)) return false;
            if (ReferenceEquals(this, other)) return true;

            return 
                (
                    Message == other.Message ||
                    Message != null &&
                    Message.Equals(other.Message)
                );
        }

        public override int GetHashCode()
        {
            unchecked
            {
                var hashCode = 41;
                    if (Message != null)
                    hashCode = hashCode * 59 + Message.GetHashCode();
                return hashCode;
            }
        }

        #region Operators
        #pragma warning disable 1591

        public static bool operator ==(InlineResponseDefault left, InlineResponseDefault right)
        {
            return Equals(left, right);
        }

        public static bool operator !=(InlineResponseDefault left, InlineResponseDefault right)
        {
            return !Equals(left, right);
        }

        #pragma warning restore 1591
        #endregion Operators
    }
}
